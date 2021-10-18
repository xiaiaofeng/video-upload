package com.idodok.video.controller;

import com.idodok.video.bo.FileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class IndexController {

    @Value("${video.upload.path}")
    private String uploadPath;

    @RequestMapping("/")
    public String index(){
        return "FileUpload";
    }

    @ResponseBody
    @RequestMapping("/getFileInfo")
    public Object getFileInfo(String fileName){
        if(fileName == null || fileName.equals("")){
            return null;
        }
        File dir = new File(uploadPath);
        if(! dir.exists()){
            dir.mkdirs();
        }
       File file = new File(dir.getAbsolutePath()+File.separator+fileName);
        FileInfo fileInfo = new FileInfo(file);
        return fileInfo;
    }

    @ResponseBody
    @RequestMapping("/uploadFile")
    public String uploadFile(@RequestParam("file") MultipartFile inFile, @RequestParam("fileName") String fileName){
        if(inFile.isEmpty()){
            return "error, file is null";
        }
        if(fileName.isEmpty()){
            return "error, fileName is null";
        }
        File dir = new File(uploadPath);
        File outFile = new File(dir.getPath()+File.separator+fileName);
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            if(! outFile.exists()){
                outFile.createNewFile();
            }
            inputStream =  inFile.getInputStream();
            //在原文件上增加，不覆盖原文件
            outputStream = new FileOutputStream(outFile, true);
            byte[] buffer = new byte[1024*1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0){
                outputStream.write(buffer, 0 , length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if(inputStream != null){
                    inputStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            try{
                if(outputStream != null){
                    outputStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return "success";
    }
}
