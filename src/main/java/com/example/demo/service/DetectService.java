package com.example.demo.service;

import com.example.demo.entity.MarkPic;
import com.example.demo.entity.Result;
import com.example.demo.entity.Task;
import com.example.demo.mapper.MarkPicMapper;
import com.example.demo.mapper.ResultMapper;
import com.example.demo.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DetectService {

    @Value("${pythonPath}")
    String pythonPath;

    MarkPicMapper markPicMapper;
    ResultMapper resultMapper;
    TaskMapper taskMapper;
    CURDService curdService;
    AutoService autoService;

    @Value("pythonPath")
    String PythonPath;

    /**
     * 项目主要功能的实现：
     * 从文件夹中获取图片，存入数据库，获取检测结果图片以及检测结果，存入数据库
     *
     * @param line  图片存放的文件夹路径
     * @return java.util.Map<java.lang.String, com.example.demo.entity.MarkPic>
     * @author SculptInk
     * -2023/3/22 20:18
     */
    public Map<String, MarkPic> detect(String[] line) {
        //识别图片
        List<List<String>> listList = autoService.judge(line);
        Map<String, MarkPic> oripath = new LinkedHashMap<>();
        //***

        //创建任务
        Task task = new Task();
        task.setCreateTime(new Date());
        int id = taskMapper.insert(task);
        int taskId = task.getId();
        //***

        //处理每张图片
        int i = 1;
        for (List<String> list :
                listList) {

            //插入标记后的图片
            MarkPic markPic = new MarkPic();
            markPic.setTaskId(taskId);
            markPic.setTaskNumber(i);
            i++;
            //获取标记后图片名称
            StringBuilder picname = new StringBuilder(list.get(0));
            picname.reverse();
            picname = new StringBuilder(picname.substring(0, picname.indexOf("\\")));
            picname.reverse();
            //***

            byte[] pic = new byte[0];
            try {
                pic = Files.readAllBytes(Paths.get(pythonPath + "\\runs\\detect\\exp\\" + picname));
            } catch (IOException e) {
                e.printStackTrace();
            }
            markPic.setMarkPic(pic);

            markPicMapper.insert(markPic);

            //***

            //插入结果
            Result result = new Result();
            result.setTaskId(markPic.getTaskId());
            result.setMarkId(markPic.getMarkId());
            //没有缺陷
            if (list.size() == 1) {
                oripath.put(list.get(0), markPic);
            } else {//***
                //有缺陷
                Iterator<String> iterator = list.iterator();
                iterator.next();
                while (iterator.hasNext()) {
                    String[] temp = iterator.next().split(",");
                    switch (temp[1]) {
                        case "A":
                            result.setDef1(Integer.parseInt(temp[0]));
                            break;
                        case "B":
                            result.setDef2(Integer.parseInt(temp[0]));
                            break;
                        case "C":
                            result.setDef3(Integer.parseInt(temp[0]));
                            break;
                        case "D":
                            result.setDef4(Integer.parseInt(temp[0]));
                            break;
                    }
                }
                //获取每张原始图片的地址
                oripath.put(list.get(0), markPic);
                //***
            }
            //***

            resultMapper.insert(result);
        }
        return oripath;

    }

    /**
     * 外层的list，每一个元素代表一张图片
     * 内层的list，[0]代表图片地址，后面的元素代表缺陷数量和类型
     *
     * @return java.util.List<java.util.List < java.lang.String>>
     * @author koveer
     * -2023/2/20 14:04
     */
    @Deprecated
    public List<List<String>> runDetect() {
        Process proc;
        List<List<String>> list = new ArrayList<>();
        try {

            String[] cmd = new String[]{pythonPath + "\\venv\\Scripts\\detect.bat"};
            proc = Runtime.getRuntime().exec(cmd, null, new File(pythonPath + "\\venv\\Scripts"));
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(), "gbk"));
            String line = null;
            while ((line = in.readLine()) != null) {
                System.out.println("********************");
                System.out.println(line);
                if (line.startsWith("image")) {
                    //temp[0] 图片位置 剩下的是缺陷数量 缺陷类型
//                    System.out.println("********************");
//                    System.out.println(line);
                    String[] ss = line.split(" ");
                    List<String> temp = new ArrayList<>();
                    temp.add(ss[2].substring(0, ss[2].length() - 1));
                    for (String s :
                            ss) {
                        if (s.startsWith("[") && s.endsWith("]"))
                            temp.add(s.substring(1, s.length() - 1));
                    }
                    list.add(temp);
                }
            }
            in.close();
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return list;
    }

}