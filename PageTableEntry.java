package osp.Memory;

import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;
/*
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.
   
   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{
    /*
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);
	   
       as its first statement.

       @OSPProject Memory
    */

    private long timeStamp;


    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
        super(ownerPageTable,pageNumber);
        this.setTimeStamp(HClock.get());
    }

    /*
       This method increases the lock count on the page by one. 

	The method must FIRST increment lockCount, THEN  
	check if the page is valid, and if it is not and no 
	page validation event is present for the page, start page fault 
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the 
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
    {

      ThreadCB iorbThread = iorb.getThread();
        
      if(!this.isValid()){

        ThreadCB pagefaultThread = this.getValidatingThread(); //Thread that caused pagefault

        if(pagefaultThread == null){

            //When page in invalid, initiate pagefault
            if(PageFaultHandler.handlePageFault(iorbThread,MemoryLock,this) == FAILURE)
                return FAILURE;
        }

        else{

            if(pagefaultThread != iorbThread){

                iorbThread.suspend(this); //wait until the page becomes valid
                if(iorbThread.getStatus() == ThreadKill )
                    return FAILURE;
            }
        }   

      }
      /*Page is valid*/

      //Incrment lock count on the frame associated with the page
      FrameTableEntry frame = this.getFrame();
      frame.incrementLockCount();

      return SUCCESS;

    }

    /** This method decreases the lock count on the page by one. 

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {
        
        FrameTableEntry frame = this.getFrame();

        //Decrment lock count on the frame associated with the page
        if(frame.getLockCount() > 0)
            frame.decrementLockCount();
        
    }

    public long getTimeStamp(){

        return timeStamp;
    }

    public void setTimeStamp(long time){

        this.timeStamp = time;
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
