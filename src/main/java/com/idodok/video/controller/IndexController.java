package com.idodok.video.controller;

import com.idodok.video.bo.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Controller
public class IndexController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${video.upload.path}")
    private String uploadPath;

    @RequestMapping("/")
    public String index() {
        return "FileUpload";
    }

    @RequestMapping("/video")
    public String video() {
        return "Video";
    }

    @ResponseBody
    @RequestMapping("/getFileInfo")
    public Object getFileInfo(String fileName) {
        if (fileName == null || fileName.equals("")) {
            return null;
        }
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir.getAbsolutePath() + File.separator + fileName);
        FileInfo fileInfo = new FileInfo(file);
        return fileInfo;
    }

    @ResponseBody
    @RequestMapping("/uploadFile")
    public String uploadFile(@RequestParam("file") MultipartFile inFile, @RequestParam("fileName") String fileName) {
        if (inFile.isEmpty()) {
            return "error, file is null";
        }
        if (fileName.isEmpty()) {
            return "error, fileName is null";
        }
        File dir = new File(uploadPath);
        File outFile = new File(dir.getPath() + File.separator + fileName);
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            if (!outFile.exists()) {
                outFile.createNewFile();
            }
            inputStream = inFile.getInputStream();
            //在原文件上增加，不覆盖原文件
            outputStream = new FileOutputStream(outFile, true);
            byte[] buffer = new byte[1024 * 10];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "success";
    }

    @GetMapping("/resource/{fileName}")
    public void downloadFile(HttpServletRequest request, HttpServletResponse response,  @PathVariable String fileName) {
        File dir = new File(uploadPath);
        File file = new File(dir.getPath() + File.separator + fileName);
        //下载开始位置
        long startByte = 0;
        //下载结束位置
        long endByte = file.length() - 1;

        //获取下载范围
        String range = request.getHeader("range");
        if (range != null && range.contains("bytes=") && range.contains("-")) {
            range = range.substring(range.lastIndexOf("=") + 1).trim();
            String rangeArray[] = range.split("-");
            if (rangeArray.length == 1) {
                //Example: bytes=1024-
                if (range.endsWith("-")) {
                    startByte = Long.parseLong(rangeArray[0]);
                } else { //Example: bytes=-1024
                    endByte = Long.parseLong(rangeArray[0]);
                }
            }
            //Example: bytes=2048-4096
            else if (rangeArray.length == 2) {
                startByte = Long.parseLong(rangeArray[0]);
                endByte = Long.parseLong(rangeArray[1]);
            }
        }

        long contentLength = endByte - startByte + 1;
        String contentType = request.getServletContext().getMimeType(fileName);
        if (contentType == null) {
            // set to binary type if MIME mapping not found
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        //HTTP 响应头设置
        //断点续传，HTTP 状态码必须为 206，否则不设置，如果非断点续传设置 206 状态码，则浏览器无法下载
        if (range != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        //Content-Range: 下载开始位置-下载结束位置/文件大小
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + startByte + "-" + endByte + "/" + file.length());
        //Content-disposition: inline; filename=xxx.xxx 表示浏览器内嵌显示该文件
        //Content-disposition: attachment; filename=xxx.xxx 表示浏览器下载该文件
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName);
        //传输文件流
        OutputStream outputStream = null;
        RandomAccessFile randomAccessFile = null;
        //已传送数据大小
        long transmittedLength = 0;
        try {
            //以只读模式设置文件指针偏移量
            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(startByte);
            outputStream = response.getOutputStream();
            byte[] buff = new byte[1024*10];
            int len;
            while (transmittedLength < contentLength && (len = randomAccessFile.read(buff)) != -1) {
                outputStream.write(buff, 0, len);
                transmittedLength += len;
            }
            response.flushBuffer();
            logger.info("下载完毕: {}-{}: {}", startByte, endByte, transmittedLength);
        } catch (IOException e) {
            logger.info("下载停止: {}-{}: {}", startByte, endByte, transmittedLength);
        } finally {
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            } catch (IOException e) {
                logger.error("文件输入流被中断");
            }
            try{
                if(outputStream != null){
                    outputStream.close();
                }
            }catch (IOException e){
                logger.error("响应输出流被中断");
            }
        }
    }
}
