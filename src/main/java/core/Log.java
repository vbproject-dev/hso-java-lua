package core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import lombok.extern.slf4j.Slf4j;
import template.LogTemplate;

@Slf4j
public class Log implements Runnable {

    private static Log instance;
    private final BlockingQueue<LogTemplate> list;
    private final BlockingQueue<LogTemplate> logsServer;
    private final Thread mythread;
    private boolean running;

    public Log() {
        list = new LinkedBlockingDeque<>();
        logsServer = new LinkedBlockingDeque<>();
        mythread = new Thread(this);
    }

    public synchronized static Log gI() {
        if (instance == null) {
            instance = new Log();
        }
        return instance;
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                LogTemplate temp = list.take();
                if (temp != null) {
                    try {
                        this.save_log(temp.name, temp.text);
                    } catch (IOException e) {
                        log.error("save log err at {} !", temp.name);
                    }
                }
                temp =null;
                temp = logsServer.take();
                if (temp != null) {
                    try {
                        this.save_log_Server(temp.name, temp.text);
                    } catch (IOException e) {
                       log.error("save log err at " + temp.name + " !");
                    }
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void save_log(String name, String text) throws IOException {
        String path = "log/" + Util.fmt_save_log.format(Date.from(Instant.now())) + "/" + name + ".txt";
        File f = new File(path);
        f.getParentFile().mkdirs();
        if (!f.exists()) {
            if (!f.createNewFile()) {
                log.info("Tạo file {}.txt xảy ra lỗi", name);
                return;
            }
        }
        try (FileWriter fwt = new FileWriter(f, true)) {
            fwt.write((text + "\n"));
        }
    }

    private void save_log_Server(String name, String text) throws IOException {
        String path = "log_server/" + Util.fmt_save_log.format(Date.from(Instant.now())) + "/" + name + ".txt";
        File f = new File(path);
        f.getParentFile().mkdirs();
        if (!f.exists()) {
            if (!f.createNewFile()) {
               log.info("Tạo file " + name + ".txt xảy ra lỗi");
                return;
            }
        }
        try (FileWriter fwt = new FileWriter(f, true)) {
            fwt.write((text + "\n"));
        }
    }

    public void start_log() {
        this.running = true;
        this.mythread.start();
    }

    public void close_log() {
        this.running = false;
        this.mythread.interrupt();
    }

    public void add_log(String name, String txt) {
        String time = "[" + Util.get_now_by_time() + "]  ";
        this.list.add(new LogTemplate(name, (time + txt)));
    }
    
    public void addLogServer(String name, String txt)
    {
        String time = "[" + Util.get_now_by_time() + "]  ";
        this.logsServer.add(new LogTemplate(name, (time + txt)));
    }
    

}
