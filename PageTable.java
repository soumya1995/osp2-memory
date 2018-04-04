package osp.Memory;
/*
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
    /*
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
        super(ownerTask);

        //Create page table
        maxPages = (int)Math.pow(2,MMU.getPageAddressBits());
        pages = new PageTableEntry[maxPages];

        //Initialize page table
        for(int i=0; i<maxPages; i++) /*not sure yet about page id*/
          pages[i] = new PageTableEntry(this, i);

    }

    /*
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {   
        //Get size of the page table
        int frameTableSize = MMU.getFrameTableSize();

        //Get the terminating task
        TaskCB task = this.getTask();

        for(int i=0; i<frameTableSize; i++){

            FrameTableEntry frame = MMU.getFrame(i);
            PageTableEntry page = frame.getPage();

            //Deallocate the pages
            if(page != null && page.getTask() == task){

                frame.setPage(null); //nullify the page field

                if(frame.isDirty())
                    frame.setDirty(false); //clean if page is dirty

                frame.setReferenced(false); //unset the refernce bit

                if(frame.getReserved() == task)
                    frame.setUnreserved(task); //Unreserve the frame if it belongs to the task

            }

        }

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
