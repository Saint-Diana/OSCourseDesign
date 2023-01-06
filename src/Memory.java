import java.util.*;

/**
 * 实现段页式内存管理的内存分配和回收。
 * 逻辑空间分段，物理空间分页。
 * 首先要输入内存大小，内存块（物理块）的大小，然后计算出内存中有几个物理块。
 * 然后输入进程的个数，每个进程的段数和段内页的个数。
 * 能够选择分配/回收操作。
 * 模拟当某进程提出申请内存空间的大小后，能够判定是否能满足要求，不满足则进行置换操作。
 * 显示每次完成分配或回收后内存空间的使用情况。
 * 我的题目要求是使用FIFO置换算法，所以需要一个队列。
 * 每个进程需要一个段表，而段表中存放的是页表。
 * 以页块为单位进行内存分配。
 *
 * @author Shen
 * @date 2022/12/13 8:48
 */
public class Memory {
    /**
     * 内存大小
     */
    private final int size;


    /**
     * 内存块大小
     */
    private final int blockSize;

    /**
     * 内存块数量
     */
    private final int blockNum;

    /**
     * 进程Map
     * key为进程名
     * value为对应的进程信息
     */
    private HashMap<String, Process> processes;


    /**
     * 内存块数组，记录内存中各个内存块的信息
     */
    private final ArrayList<Block> blocks;


    /**
     * 正在使用的内存块队列，用于FIFO置换算法
     * 按照内存块使用的先后顺序进行排列，那么每次置换都将队首内存块（也就是在内存中驻留时间最长的页置换出来）
     */
    private Queue<Integer> queue;

    /**
     * 进程类
     */
    static class Process {
        /**
         * 该进程段数
         */
        private int segmentNum;

        /**
         * 每段对应占用的内存大小
         */
        private ArrayList<Integer> segmentSize;

        /**
         * 段表
         */
        private ArrayList<Segment> segments;
    }

    /**
     * 物理块
     */
    static class Block {
        /**
         * 块内存放的逻辑页属于的进程名
         */
        private String processName;

        /**
         * 块内存放的逻辑页属于的段号
         */
        private int segmentId;

        /**
         * 块内存放的逻辑页属于的页号
         */
        private int pageId;

        /**
         * 物理块是否空闲
         */
        private boolean isFree;

        /**
         * 内存块内真正被使用的内存大小
         */
        private int realUsedSize;

        public Block(boolean isFree) {
            this.isFree = isFree;
        }
    }

    /**
     * 段
     */
    static class Segment {
        /**
         * 页表长度
         */
        private int length;

        /**
         * 页表始址
         */
        private int head;

        /**
         * 页表
         */
        private ArrayList<Page> pages;

        public Segment(int length, int head) {
            this.length = length;
            this.head = head;
            pages = new ArrayList<>();
            for(int i = 0;i < length;++i){
                pages.add(new Page());
            }
        }
    }

    /**
     * 页
     */
    static class Page {
        /**
         * 块号，也就是在内存中存放的位置
         */
        private int blockId;

        public Page(int blockId) {
            this.blockId = blockId;
        }

        public Page() {
        }
    }


    /**
     * 构造函数，内存大小必须是物理块大小的整数倍
     *
     * @param size 给定内存大小
     * @param blockSize 给定内存块大小
     */
    public Memory(int size, int blockSize){
        this.size = size;
        this.blockSize = blockSize;
        this.blockNum = size / blockSize;
        blocks = new ArrayList<>();
        queue = new LinkedList<>();
        processes = new HashMap<>();
        //初始化时所有的内存块都是空闲块
        for(int i = 0;i < blockNum;++i){
            blocks.add(new Block(true));
        }
    }

    /**
     * 根据输入的进程名，查找进程集合中是否存在该进程，如果不存在，则将其添加到进程集合当中，并且对其中的段表进行初始化：
     *      需要知道进程名，该进程的段数，以及每段的大小
     *
     * @param processName 进程名
     * @param segmentNum 段数
     * @param segmentSize 每段的大小
     */
    public void allocation(String processName, int segmentNum, ArrayList<Integer> segmentSize){
        if(processes.containsKey(processName)){
            //如果进程Map中已经有这个进程了，那么就不需要进行初始化
            //而是需要在这个进程的段表里再加上新的表项
            Process process = processes.get(processName);
            //原先有oldNum个表项
            int oldNum = process.segmentNum;
            //现在变成了oldNum + segmentNum个表项
            process.segmentNum = oldNum + segmentNum;
            //同时把每个新增的段对应的段的大小也加入进去
            process.segmentSize.addAll(segmentSize);
            //那么对应的需要修改段表
            int address = process.segments.get(oldNum - 1).head + process.segments.get(oldNum - 1).length;
            for(int i = 0;i < segmentNum;++i){
                //每段含有的页数，例如当前段大小为29kb，而一个页面的大小为20kb，则当前段分为2页
                int pageNum = segmentSize.get(i) % blockSize == 0 ? segmentSize.get(i) / blockSize : segmentSize.get(i) / blockSize + 1;
                process.segments.add(new Segment(pageNum, address));
                address += pageNum;
            }
            processes.put(processName, process);
            //然后对新增的段表项中的每个页表中的页进行内存分配
            for(int i = oldNum;i < process.segmentNum;++i){
                Segment currentSegment = process.segments.get(i);
                if(process.segmentSize.get(i) % blockSize == 0){
                    for(int j = 0;j < currentSegment.length;++j){
                        doAllocation(processName, i, j, blockSize);
                    }
                }else{
                    //这种情况是内存块存在内碎片！
                    for(int j = 0;j < currentSegment.length - 1;++j){
                        doAllocation(processName, i, j, blockSize);
                    }
                    doAllocation(processName, i, currentSegment.length - 1, process.segmentSize.get(i) % blockSize);
                }
            }
        }else{
            //否则需要把这个进程加入到进程Map当中，并且初始化进程的段表和段表对应的页表
            Process process = new Process();
            process.segmentNum = segmentNum;
            process.segmentSize = segmentSize;
            process.segments = new ArrayList<>();
            int address = 0;
            for(int i = 0;i < segmentNum;++i){
                //每段含有的页数，例如当前段大小为29kb，而一个页面的大小为20kb，则当前段分为2页
                int pageNum = segmentSize.get(i) % blockSize == 0 ? segmentSize.get(i) / blockSize : segmentSize.get(i) / blockSize + 1;
                process.segments.add(new Segment(pageNum, address));
                address += pageNum;
            }
            processes.put(processName, process);
            //然后对段表中的每个页表中的页进行内存分配
            for(int i = 0;i < process.segmentNum;++i){
                Segment currentSegment = process.segments.get(i);
                if(segmentSize.get(i) % blockSize == 0){
                    for(int j = 0;j < currentSegment.length;++j){
                        doAllocation(processName, i, j, blockSize);
                    }
                }else{
                    //这种情况是内存块存在内碎片！
                    for(int j = 0;j < currentSegment.length - 1;++j){
                        doAllocation(processName, i, j, blockSize);
                    }
                    doAllocation(processName, i, currentSegment.length - 1, segmentSize.get(i) % blockSize);
                }
            }
        }
    }

    /**
     * 内存分配：
     *      以页块为单位进行分配
     * 需要知道这个页所属的进程名，段号，以及在段中的页号
     * 内存块大小为blockSize
     *
     * @param processName 进程名
     * @param segmentId 段号
     * @param pageId 页号
     * @param size 页块的大小（因为可能有些段最后一页的大小不等于blockSize）
     */
    private void doAllocation(String processName, int segmentId, int pageId, int size) {
        //当前进行分配的进程
        Process currentProcess = processes.get(processName);
        //遍历内存中的内存块，如果有空闲内存块，就将该页分配给它
        //如果所有内存块都被占用，那么进行置换
        for(int i = 0;i < blocks.size();++i){
            Block currentBlock = blocks.get(i);
            if(currentBlock.isFree){
                //如果有空闲块，就直接分配
                //修改内存块数组中的信息
                currentBlock.isFree = false;
                currentBlock.processName = processName;
                currentBlock.segmentId = segmentId;
                currentBlock.pageId = pageId;
                currentBlock.realUsedSize = size;
                //修改当前进程的段表对应页表中的信息
                Page currentPage = currentProcess.segments.get(segmentId).pages.get(pageId);
                currentPage.blockId = i;
                //将该内存块的块号添加到队列中
                queue.add(i);
                System.out.println("进程" + processName + "成功分配" + size + "KB内存");
                showBlocks();
                return;
            }
        }
        //没有空闲块，则需要使用FIFO置换规则释放queue中第一个内存块，并将其分配给当前页
        Integer blockId = queue.remove();
        System.out.println("当前内存已满，需要进行置换，将内存块" + blockId + "释放");
        free(blockId);
        doAllocation(processName, segmentId, pageId, size);
    }

    /**
     * 回收某个内存块
     *      1、修改内存块数组blocks，修改isFree为true
     *      2、修改进程对应的段表
     *
     * @param blockId 内存块号
     */
    public void free(int blockId){
        if(blockId >= blockNum || blockId < 0){
            System.out.println("该内存块越界！");
            return;
        }
        Block currentBlock = blocks.get(blockId);
        if(currentBlock.isFree){
            System.out.println("内存块" + blockId + "未被分配，无需回收");
        }else{
            String processName = currentBlock.processName;
            int segmentId = currentBlock.segmentId;
            int pageId = currentBlock.pageId;
            int realUsedSize = currentBlock.realUsedSize;
            //找到对应页表
            ArrayList<Page> pages = processes.get(processName).segments.get(segmentId).pages;
            //然后将其中页号对应的块号修改为-1
            pages.get(pageId).blockId = -1;
            //然后修改内存块数组中的信息
            currentBlock.isFree = true;
            currentBlock.processName = null;
            currentBlock.segmentId = 0;
            currentBlock.pageId = 0;
            currentBlock.realUsedSize = 0;
            System.out.println("内存块" + blockId + "被释放,成功释放" + realUsedSize + "kb内存");
        }
    }

    /**
     * 打印内存块信息
     */
    public void showBlocks(){
        System.out.println("块号\t   进程名\t  段号\t  页号\t  内存块大小\t  实际使用大小\t  空闲状态");
        for(int i = 0;i < blocks.size();++i){
            Block currentBlock = blocks.get(i);
            System.out.println(i + "\t\t " + currentBlock.processName + "\t\t " + currentBlock.segmentId +
                    "\t\t " + currentBlock.pageId + "\t\t " + blockSize + "\t\t\t " + currentBlock.realUsedSize + "\t\t\t " + currentBlock.isFree);
        }
    }

    /**
     * 打印进程对应的段表信息
     *
     * @param processName 进程名
     */
    public void showSegments(String processName) {
        Process currentProcess = processes.get(processName);
        if(currentProcess == null){
            System.out.println("该进程不存在！");
            return;
        }
        System.out.println("当前进程" + processName + "的段表信息如下：");
        System.out.println("段号\t 页表长度\t 页表始址");
        for(int i = 0;i < currentProcess.segmentNum;++i){
            Segment currentSegment = currentProcess.segments.get(i);
            System.out.println(i + "\t\t " + currentSegment.length + "\t\t " + currentSegment.head);
        }
        //输出段号对应页表信息
        for(int i = 0;i < currentProcess.segmentNum;++i){
            Segment currentSegment = currentProcess.segments.get(i);
            System.out.println("段号" + i + "对应的页表信息如下：");
            System.out.println("页号\t 块号\t");
            for(int j = 0;j < currentSegment.length;++j){
                System.out.println(j + "\t\t " + currentSegment.pages.get(j).blockId);
            }
        }
    }

    public void showQueue(){
        if(queue.isEmpty()){
            System.out.println("当前没有正在使用的内存块！");
            return;
        }
        System.out.println("当前正在使用的内存块号队列如下：");
        for(Integer blockId : queue){
            System.out.print("内存块" + blockId + "  ");
        }
        System.out.println();
    }
}
