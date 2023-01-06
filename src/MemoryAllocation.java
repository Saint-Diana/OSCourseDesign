import java.util.ArrayList;
import java.util.Scanner;

/**
 * @author Shen
 * @date 2022/12/13 9:23
 */
public class MemoryAllocation {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("请输入内存大小：");
        int memorySize = sc.nextInt();
        System.out.print("请输入内存块大小：");
        int blockSize = sc.nextInt();
        Memory my = new Memory(memorySize, blockSize);
        while (true) {
            System.out.println("请选择以下操作：");
            System.out.print("1.分配内存\n2.释放内存\n3.展示分区状况\n4.展示进程对应段表\n5.展示当前队列中的内存块号\n6.退出程序\n");
            System.out.print("输入：");
            int n = sc.nextInt();
            switch (n) {
                case 1: {
                    System.out.println("请输入进程名：");
                    String processName = sc.next();
                    System.out.print("请输入该进程段数：");
                    int segmentNum = sc.nextInt();
                    System.out.println("请依次输入段的大小：");
                    ArrayList<Integer> arr = new ArrayList<>();
                    for(int i = 0;i < segmentNum;++i){
                        int t = sc.nextInt();
                        arr.add(t);
                    }
                    my.allocation(processName, segmentNum, arr);// 调用对象Memory中内存分配函数allocation
                    break;
                }
                case 2: {
                    System.out.print("请输入释放的内存块号：");
                    int id = sc.nextInt();
                    my.free(id);// 调用对象Memory中内存释放函数collection
                    break;
                }
                case 3: {
                    my.showBlocks();// 展示分区状况
                    break;
                }
                case 4: {
                    System.out.println("请输入进程名：");
                    String processName = sc.next();
                    my.showSegments(processName);
                    break;
                }
                case 5: {
                    my.showQueue();
                    break;
                }
                case 6: {
                    System.out.println("退出成功！");
                    System.exit(0);
                }
                default:
                    System.out.println("输入错误，请重新输入");
            }
        }
    }
}
