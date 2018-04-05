package osp.Memory;

import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/*
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /* 
        This method is called once before the simulation starts. 
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {   
        //Initialize the frame table
        for(int i=0; i<MMU.getFrameTableSize(); i++)
            MMU.setFrame(i, new getFrameTableEntry(i));

        //Initialize page fault handler
        PageFaultHandler.init();   /*NOT SURE IF WE NEED THIS*/

    }

    /*
       This method handlies memory references. The method must 
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault 
       by making an interrupt if the page is invalid, finally, 
       if the page is still valid, i.e., not swapped out by another 
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue, 
       and it is possible that some other thread will take away the frame.)
       
       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform 
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,
					  int referenceType, ThreadCB thread)
    {
        //Determine the referenced page
        int addressBits = MMU.getVirtualAddressBits(); //Get the number of bits in the logical address
        int pageBits = MMU.getPageAddressBits(); //Get the number of bits in the page number portion of address
        int offset = addressBits - pageBits; //Get the number of bits in offset of a page
        int pageSize = (int)Math.pow(2,offset); //Get the page size
        int pageNumber = memoryAddress/pageSize; 
        PageTableEntry page = MMU.getPTBR().pages[pageNumber]; //The refernced page

        //Page is valid
        /*if(page.isValid()){

            FrameTableEntry frame = page.getFrame();

            //Set the refernced and dirty bits
            frame.setReferenced(true);
            if(referenceType == MemoryWrite)
                frame.setDirty(true);
            else
                frame.setDirty(false);

            return page;
        }*/

        //Page is invalid
        if(!page.isValid()){

            ThreadCB validateThread = page.getValidatingThread();

            /*CASE 1*/
            if(validateThread != null){
                if(validateThread != thread && thread != null){

                    thread.suspend(page);
                    if(thread.getStatus() == ThreadKill) //If the thread was destroyed
                        return page;
                }
            }
            /*CASE 2*/
            else{

                InterruptVector.setPage(page);
                InterruptVector.setReferenceType(referenceType);
                InterruptVector.setThread(thread);
                CPU.interrupt(PageFault);

                if(thread.getStatus() == ThreadKill) //If the thread was destroyed
                        return page;
            }
        }

        FrameTableEntry frame = page.getFrame();

        //Set the refernced and dirty bits
        frame.setReferenced(true);
        if(referenceType == MemoryWrite)
            frame.setDirty(true);
        else
            frame.setDirty(false);

        +return page;


    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
     
	@OSPProject Memory
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
      @OSPProject Memory
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
