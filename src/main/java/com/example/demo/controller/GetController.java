package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.entity.*;
import com.example.demo.mapper.*;
import com.example.demo.service.CURDService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@Api
@CrossOrigin
@RequestMapping("/api")
@RequiredArgsConstructor
public class GetController {
    CURDService curdService;
    TaskMapper taskMapper;
    ResultMapper resultMapper;
    OriPicMapper oriPicMapper;
    MarkPicMapper markPicMapper;
    DetectResultMapper detectResultMapper;
    MsgMapper msgMapper;

    /**
     * 前端根据图片id获取原始图片.
     *
     * @param id 图片id
     * @param resp http请求
     * @author koveer
     * -2023/2/8 16:40
     */
    @GetMapping(value = "/OriImage/{id}", produces = "application/json;charset=utf-8")
    public void getOriImage(@PathVariable("id") Integer id, HttpServletResponse resp) throws Exception {
        OriPic oriPic = oriPicMapper.selectById(id);
        byte[] image = oriPic.getOriPic();
        ServletOutputStream out = resp.getOutputStream();
        out.write(image);
        out.flush();
        out.close();
    }

    /**
     * 根据图片id获取标记图片
     *
     * @param id 图片id
     * @param resp http请求
     * @author koveer
     * -2023/2/8 16:41
     */
    @GetMapping(value = "/MarkImage/{id}", produces = "application/json;charset=utf-8")
    public void getMarkImage(@PathVariable("id") Integer id, HttpServletResponse resp) throws Exception {
        MarkPic markPic = markPicMapper.selectById(id);
        byte[] image = markPic.getMarkPic();
        resp.setContentType("image/jpeg");
        ServletOutputStream out = resp.getOutputStream();
        out.write(image);
        out.flush();
        out.close();
    }

    /**
     * 通过时间json字符串获取检测结果列表
     *
     * @param date 起始时间和结束时间
     * @return java.lang.String
     * @author koveer
     * -2023/2/8 14:35
     */
    @PostMapping(value = "/date", produces = "application/json; utf-8")
    public String getbydate(@RequestBody String date) throws ParseException {
        JSONObject jsonObject = JSON.parseObject(date);
        String beginDateStr = (String) jsonObject.get("beginDate");
        String endDateStr = (String) jsonObject.get("endDate");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date beginDate = sdf.parse(beginDateStr);
        Date endDate = sdf.parse(endDateStr);
        if (beginDate.after(endDate))
            return JSONObject.toJSONString(new Message(MessageCode.ERROR, null));
        Result[] results = curdService.searchByTime(beginDate, endDate);
        return JSONObject.toJSONString(new Message(MessageCode.SUCCESS, results));
    }

    /**
     * 通过时间json字符串检测结果获取检测结果列表
     *
     * @param str 缺陷类型json字符串
     * @return java.lang.String
     * @author koveer
     * -2023/2/8 14:37
     */
    @PostMapping(value = "/description", produces = "application/json; utf-8")
    public String getbydefect(@RequestBody String str) {
        JSONObject jsonObject = JSON.parseObject(str);
        JSONArray defect = jsonObject.getJSONArray("defect");
        List<String> list = new ArrayList<>();
        for (Object object :
                defect) {
            String s = (String) object;
            list.add(s);
            if (!(s.equals("def1") || s.equals("def2") || s.equals("def3") || s.equals("def4")))
                return JSONObject.toJSONString(new Message(MessageCode.ERROR, null));
        }
        List<Result> results = curdService.searchByResult(list);
        return JSONObject.toJSONString(new Message(MessageCode.SUCCESS, results));
    }

    /**
     * 获取所有任务列表
     *
     * @return java.lang.String
     * @author koveer
     * -2023/2/8 14:38
     */
    @GetMapping(value = "/task", produces = "application/json;charset=utf-8")
    public String getAllTask() {
        Task[] tasks = taskMapper.selectAll();
        return JSONObject.toJSONString(new Message(MessageCode.SUCCESS, tasks));
    }

    /**
     * 获取所有结果列表
     *
     * @return java.lang.String
     * @author koveer
     * -2023/2/8 14:38
     */
    @GetMapping(value = "/result", produces = "application/json;charset=utf-8")
    public String getAllResult() {
        Result[] results = resultMapper.selectAll();
        return JSONObject.toJSONString(new Message(MessageCode.SUCCESS, results));
    }

    /**
     * 获取全部检测结果
     *
     * @return java.lang.String
     * @author koveer
     * -2023/2/8 16:22
     */
    @GetMapping(value = "/detect", produces = "application/json;charset=utf-8")
    public String getDetect() {
        DetectResult[] detectResults = detectResultMapper.selectAllDetect();
        return JSONObject.toJSONString(new Message(MessageCode.SUCCESS, detectResults));
    }

    /**
     * 根据任务id获取某次任务的全部信息
     *
     * @param id 任务序号
     * @return java.lang.String
     * @author koveer
     * -2023/2/8 16:37
     */
    @GetMapping(value = "/detect/{id}", produces = "application/json;charset=utf-8")
    public String getDetect(@PathVariable Integer id) {
        DetectResult[] detectResult = detectResultMapper.selectAllDetectByTaskId(id);
        return JSONObject.toJSONString(new Message(MessageCode.SUCCESS, detectResult));
    }

    /**
     * 返回最新一条Task数据
     *
     * @return java.lang.String
     * @author koveer
     * -2023/3/20 14:32
     */
    @GetMapping(value = "/new", produces = "application/json;charset=utf-8")
    public String getNew() {
        NewTask[] newTask = taskMapper.getNew();
        return JSONObject.toJSONString(new Message(MessageCode.SUCCESS, newTask));
    }

    /**
     * 返回全部任务的每种缺陷的计数
     *
     * @return java.lang.String
     * @author koveer
     * -2023/3/20 14:32
     */
    @GetMapping(value = "/total", produces = "application/json;charset=utf-8")
    public String getTotal() {
        Total total = new Total();
        total.setCon(taskMapper.countById());
        total.setDef1(resultMapper.countByDef1());
        total.setDef2(resultMapper.countByDef2());
        total.setDef3(resultMapper.countByDef3());
        total.setDef4(resultMapper.countByDef4());
        total.setDef0(resultMapper.countByDef0());
        return JSONObject.toJSONString(new Message(MessageCode.SUCCESS, total));
    }

    @PostMapping(value = "/test")
    public String test(@RequestBody User user) {
        return user.toString();
    }

    /**
     * 数据库录入成功后更新msg id
     *
     * @return java.lang.String
     * @author koveer
     * -2023/3/20 14:32
     */
    @GetMapping("/msg")
    public String msg() {
        return JSONObject.toJSONString(new Message(MessageCode.SUCCESS, msgMapper.getNew()));
    }
}