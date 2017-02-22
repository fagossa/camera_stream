package fr.xebia.streams.java.common;

public class Dimensions {
    Integer width;
    Integer height;

    public Dimensions(Integer width, Integer height){
        this.width=width;
        this.height=height;
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
}
