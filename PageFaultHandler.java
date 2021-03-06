package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/*
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /*
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {   
        //Page is valid
        if(page.isValid())
            return FAILURE;

        if(page == null)
            return FAILURE;

        //Get size of the frame table
        int frameTableSize = MMU.getFrameTableSize();

        int frameCount = 0;
        for(int i=0; i<frameTableSize; i++){

            FrameTableEntry frame = MMU.getFrame(i); 
            if(frame.getLockCount() != 0 || frame.isReserved() == true)
                frameCount++;          
        }
        //No frame is available
        if(frameTableSize == frameCount)
            return NotEnoughMemory;

        //Create event object
        SystemEvent event = new SystemEvent("PageFault");
        thread.suspend(event);

        //Set the validating thread of the page
        page.setValidatingThread(thread);

        //Find a suitable frame for the page
        FrameTableEntry frame = findFrameLRU();

        //Reserve the frame for that task
        frame.setReserved(thread.getTask());

        if(frame.getPage() != null){
            PageTableEntry prevPage = frame.getPage();
            if(frame.isDirty() == true){
                //Perform swap out
                OpenFile swapFileOut = frame.getPage().getTask().getSwapFile();
                swapFileOut.write(frame.getPage().getID(), frame.getPage(), thread);
            

                if(thread.getStatus() == ThreadKill){ //If the thread was destroyed
                    
                    event.notifyThreads();
                    page.notifyThreads();
                    //page.setValidatingThread(null);
                    ThreadCB.dispatch();
                    return FAILURE;
                }

                frame.setDirty(false);

            }

            frame.setPage(null); //free the frame
            frame.setReferenced(false);

            prevPage.setValid(false);
            prevPage.setFrame(null);
        }

        //Set page's frame attributes
        /*frame.setDirty(false);
        frame.setReferenced(false);*/

        //Set the page to the frame and the validating thread of the page
        page.setFrame(frame);
        
        //Perform swap in
        OpenFile swapFile = page.getTask().getSwapFile();
        swapFile.read(page.getID(), page, thread);

        if(thread.getStatus() == ThreadKill){ //If the thread was destroyed

            if(frame.getPage() != null){/****************/

                if(frame.getPage().getTask() == thread.getTask())
                    frame.setPage(null);
            
                page.setValidatingThread(null);
                page.setFrame(null);
                page.notifyThreads();
                event.notifyThreads();
                ThreadCB.dispatch();
                return FAILURE;
            }

        }

        frame.setPage(page);
        page.setValid(true);

        //Set the refernced and dirty bits

        frame.setReferenced(true);
        if(referenceType == MemoryWrite)
            frame.setDirty(true);
        
        if(frame.getReserved() == thread.getTask())
            frame.setUnreserved(thread.getTask());
        page.notifyThreads();
        event.notifyThreads();
        page.setValidatingThread(null);
        ThreadCB.dispatch();
        return SUCCESS;

    }

    public static FrameTableEntry findFrameLRU(){

        //Get size of the frame table
        int frameTableSize = MMU.getFrameTableSize();

        //If a frame is empty return it
        for(int i=0; i<frameTableSize; i++){

            FrameTableEntry frame = MMU.getFrame(i); 
            if(frame.getPage() == null && frame.getReserved() == null)
                return frame;
        }

        //If a frame is not empty find suitable frame according to LRU; the page least recently referenced
        long maxTimeElapsed = 0;
        FrameTableEntry replaceFrame = null;
        for(int i=0; i<frameTableSize; i++){

            FrameTableEntry frame = MMU.getFrame(i);
            PageTableEntry page = frame.getPage();
            long timeElapsed = Math.abs(HClock.get()-page.getTimeStamp());

            if(timeElapsed > maxTimeElapsed && frame.getReserved() == null && frame.getLockCount() == 0){

                replaceFrame = frame;
                maxTimeElapsed = timeElapsed;
            }
        }
        if(replaceFrame.getReserved() != null)
            System.out.println("resuic");
        return replaceFrame;

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
