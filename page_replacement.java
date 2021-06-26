import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

class Memory {
    class Frame{
        private int frame_data;
        private boolean ref_bit, modify_bit;
        
        public Frame(){
            frame_data=0;
            ref_bit=false;
            modify_bit=false;
        }
    }
    private final int num_of_data=100000, range_of_data=500;
    private int normal_data[] = new int[num_of_data];
    private int Locality_data[] = new int[num_of_data];
    private int Gaussian_data[] = new int [num_of_data];
    private boolean need_to_write[] =new boolean[num_of_data];
    private int data[];
    private int num_of_frame;
    private Frame frame[];
    private Random r = new Random(System.nanoTime());
    

    public Memory() {
        for (int i = 0; i < num_of_data; i++){    //random 1~500 number then put it in data
            do{
                normal_data[i] = r.nextInt(range_of_data+1);
                need_to_write[i] = r.nextBoolean();
                Gaussian_data[i]=(int)(r.nextGaussian()*100+250);  //gaussian distribution mean=250, dev=100
            }while(normal_data[i]<1 || Gaussian_data[i]<1 || Gaussian_data[i]>500);     //data can't be zero and range is 1~500
        }
        Locality_data = Locality_Data();
    }

    private void FIFO(){
        int page_fault = 0, victim=0, frame_use_count=0, interrupt=0, write_in_disk=0;
        boolean page_state_of_use[] = new boolean[range_of_data];

        for(int i=0;i<frame.length;i++)
            frame[i].frame_data=0;     //clear Frame space

        for(int i=0;i<num_of_data;i++){  
            int page_state_index = data[i] -1;
            //if frame has space && page not in frame
            if(frame_use_count<num_of_frame && !page_state_of_use[page_state_index]){
                frame[frame_use_count].frame_data = data[i];
                frame[frame_use_count].modify_bit = need_to_write[i];
                page_state_of_use[page_state_index]=true;
                page_fault++;   interrupt++;    frame_use_count++;
            //frame is filled && page not in frame
            }else if(!page_state_of_use[page_state_index]){     
                int old_page_state_index = frame[victim].frame_data-1;
                page_state_of_use[old_page_state_index]=false;
                if(frame[victim].modify_bit){
                    interrupt++;    write_in_disk++;
                }
                frame[victim].frame_data = data[i];
                frame[victim].modify_bit = need_to_write[i];
                page_state_of_use[page_state_index]=true;
                page_fault++;   interrupt++;    victim++;
                if(victim==num_of_frame)
                    victim=0;
            }
        }
        // System.out.println("FIFO Frame Number: "+ num_of_frame +", page fault: " + page_fault +
        //                     ", interrupt: " + interrupt + ", disk writes: " + write_in_disk);
        System.out.println(write_in_disk);
    }

    private void OPT(){
        int page_fault = 0, frame_use_count=0, interrupt=0, write_in_disk=0;
        int page_state_of_use[] = new int [range_of_data];
        Arrays.fill(page_state_of_use, -1);

        for(int i=0;i<num_of_data;i++){
            int page_state_index=data[i]-1;
            if(frame_use_count<num_of_frame && page_state_of_use[page_state_index]==-1){      //frames has space and page aren't in frame
                frame[frame_use_count].frame_data=data[i];
                frame[frame_use_count].modify_bit=need_to_write[i];
                page_state_of_use[page_state_index]=frame_use_count;
                frame_use_count++;  page_fault++;   interrupt++;
            }else if(page_state_of_use[page_state_index]==-1){  //page are not in frame
                int count=0,swap_index=0;
                boolean predict_tag[] = new boolean[range_of_data];
                
                for(int j=i+1;j<num_of_data;j++){   //search and predict
                   int next_data_record_index = data[j]-1; 
                   //if the page is in frame && never be predicted
                   if(page_state_of_use[next_data_record_index]!=-1 && !predict_tag[next_data_record_index]){
                       predict_tag[next_data_record_index]=true;
                       count++;
                       swap_index = page_state_of_use[next_data_record_index];
                       if(count==num_of_frame)  //predict enough page
                            break;
                   }
                }
                if(count<num_of_frame){    //no prediction, select frame[0] to swap
                    for(int j=0;j<num_of_frame;j++){
                        int index_temp=frame[j].frame_data-1;
                        if(!predict_tag[index_temp])
                            swap_index=j;
                    }
                }

                if(frame[swap_index].modify_bit){
                    interrupt++;    write_in_disk++;
                }
                //cancel the origin data record and set false
                page_state_of_use[frame[swap_index].frame_data - 1]=-1;      
                frame[swap_index].frame_data=data[i];
                frame[swap_index].modify_bit=need_to_write[i];
                page_state_of_use[page_state_index]=swap_index;
                interrupt++;   page_fault++; 
            }
        }
        // System.out.println("OPT Frame Number: "+ num_of_frame +", page fault: " + page_fault +
        //                     ", interrupt: " + interrupt + ", disk writes: " + write_in_disk);
        System.out.println(write_in_disk);
    }

    private void Enhanced_Second_Chance(){
        int page_fault=0, victim=0, frame_use_count=0, swap_index=0, interrupt=0, write_in_disk=0;
        boolean page_state_of_use[] = new boolean[range_of_data];

        for(int i=0;i<num_of_data;i++){
            boolean search_mode=true;
            int page_state_index = data[i]-1;
            if(victim == num_of_frame)
                victim=0;
            //frame has space && page is not in frame
            if(frame_use_count<num_of_frame && !page_state_of_use[page_state_index]){
                frame[frame_use_count].frame_data = data[i];
                frame[frame_use_count].ref_bit = true;
                frame[frame_use_count].modify_bit = need_to_write[i];
                page_state_of_use[page_state_index] = true;
                frame_use_count++;  interrupt++;    page_fault++;
            }else if(!page_state_of_use[page_state_index]){     //if page is not in frame
                boolean found = false;
                int circular_start = victim, count;
                //first loop for searching (0,0)
                while(true){
                    count=0;
                    for(int j=circular_start;;j++){
                        if(j == num_of_frame)
                            j=0;
                        if(search_mode){     //if search_mode is true, search (0.0)
                            if(frame[j].ref_bit == false && frame[j].modify_bit == false){      //if find (0.0)
                                swap_index = j;
                                found = true;
                                break;
                            }
                        }else{
                            frame[j].ref_bit=false;
                            if(frame[j].ref_bit==false && frame[j].modify_bit == true){
                                swap_index = j;
                                found=true;
                                break;
                            }
                        }
                        count++;
                        if(count==num_of_frame){
                            search_mode = !search_mode;
                            break;
                        }
                    }

                    if(found){
                        if(frame[swap_index].modify_bit){
                            interrupt++;    write_in_disk++;
                        }
                        page_state_of_use[frame[swap_index].frame_data - 1]=false;      
                        frame[swap_index].frame_data = data[i];
                        frame[swap_index].ref_bit = true;
                        frame[swap_index].modify_bit = need_to_write[i];
                        page_state_of_use[page_state_index]=true;
                        victim=swap_index+1;    interrupt++;    page_fault++;
                        break;
                    }
                }
            }
        }
        // System.out.println("Enhanced Second Chance Frame Number: "+ num_of_frame +", page fault: "
        //                      + page_fault +", interrupt: " + interrupt + ", disk writes: " + write_in_disk);

        System.out.println(write_in_disk);
    }

    private void myAlgorithm(){
        int page_fault=0, interrupt=0, write_in_disk=0, frame_use_count=0;
        boolean page_state_of_use[] = new boolean[range_of_data];
        int page_statistic_of_use[] = new int[range_of_data];
        int weight[] = new int[range_of_data];
        boolean weight_record[] = new boolean[range_of_data];

        for(int i=0;i<num_of_data;i++){
            int page_state_index = data[i]-1;

            if(i%(num_of_frame*5)==0){
                Arrays.fill(page_statistic_of_use, 0);
                Arrays.fill(weight_record,false);
            }
            if(i%(num_of_frame*10)==0)
                Arrays.fill(weight, 0);

            if(frame_use_count<num_of_frame && !page_state_of_use[page_state_index]){
                frame[frame_use_count].frame_data = data[i];
                frame[frame_use_count].modify_bit = need_to_write[i];
                page_state_of_use[page_state_index] = true;
                page_statistic_of_use[page_state_index]++;
                if(!weight_record[page_state_index]){
                    weight[page_state_index]++;
                    weight_record[page_state_index]=true;
                }
                frame_use_count++;  interrupt++;    page_fault++;
            }else if(!page_state_of_use[page_state_index]){
                int swap_index=0, page_index=0;
                double min_of_use;
                min_of_use = page_statistic_of_use[frame[0].frame_data-1]* (1+(double)weight[frame[0].frame_data-1] / 100);

                for(int j=1;j<num_of_frame;j++){
                    page_index = frame[j].frame_data-1;
                    if(min_of_use> page_statistic_of_use[page_index]* (1+(double)weight[page_index] / 100)){
                        swap_index=j;
                        // min_of_use = page_statistic_of_use[page_index];
                        min_of_use = page_statistic_of_use[page_index]* (1+(double)weight[page_index] / 100);
                    }
                }
                if(frame[swap_index].modify_bit){
                    interrupt++;    write_in_disk++;
                }
                page_state_of_use[frame[swap_index].frame_data-1]=false;
                frame[swap_index].frame_data = data[i];
                frame[swap_index].modify_bit=need_to_write[i];
                page_state_of_use[page_state_index] = true;
                page_statistic_of_use[page_state_index]++;
                page_fault++; interrupt++; 
            }else if(page_state_of_use[page_state_index]){
                page_statistic_of_use[page_state_index]++;
                if(!weight_record[page_state_index]){
                    weight[page_state_index]++;
                    weight_record[page_state_index]=true;
                }
            }
        }

        // System.out.println("My Algorithm Frame Number: "+ num_of_frame +", page fault: "
        //                      + page_fault +", interrupt: " + interrupt + ", disk writes: " + write_in_disk);
        System.out.println(write_in_disk);
    }

    private int[] Locality_Data(){
        int count=0, temp=0;
        int divide_number=0, page_range=0, page_index=0, num_of_func_page=0;
        int data[]=new int [num_of_data];
            do{
                if(count==temp+num_of_func_page){
                    divide_number = r.nextInt(11)+10;    //10~20
                    page_range = range_of_data/divide_number;
                    page_index = r.nextInt(range_of_data-page_range)+1; //page start's index
                    num_of_func_page = r.nextInt(500)+500+1;    //500~1000 page number 
                    temp=count;
                }
                data[count]=r.nextInt(page_range)+page_index;
                count++;
            }while(count<num_of_data);
        return data;
    }

    public int Algorithm_test(){
        while(true){
            System.out.println("Please input number of algorithm(1.FIFO 2.OPT 3.Ehanced Second Chance 4.myAlgorithm): ");
            Scanner scn = new Scanner(System.in);
            int algorithm_num = scn.nextInt();
            System.out.println("Please input test data(1. normal 2.locality 3.myData): ");
            int data_num = scn.nextInt();
            switch(data_num){
                case 1: data=normal_data;   break;
                case 2: data=Locality_data;   break; 
                case 3: data=Gaussian_data;     break;
            }
            for(int i=10;i<=100;i+=10){
                num_of_frame=i;
                frame = new Frame[num_of_frame];
                for(int j=0;j<frame.length;j++)
                    frame[j]=new Frame();
                switch (algorithm_num){
                    case 1: FIFO();     break;
                    case 2: OPT();      break;
                    case 3: Enhanced_Second_Chance();       break;
                    case 4: myAlgorithm();      break;
                }
            }
        }
    }
}

public class page_replacement {
    public static void main(String args[]) {
        Memory memory = new Memory();
        memory.Algorithm_test();
    }
}
