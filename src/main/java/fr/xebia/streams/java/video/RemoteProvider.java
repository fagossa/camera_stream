package fr.xebia.streams.java.video;

public class RemoteProvider {
    Integer width;
    Integer height;
    String url;

    public RemoteProvider() {
        this.width = 512;
        this.height = 288;
        this.url = "http://$host/html/cam_pic_new.php";
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
