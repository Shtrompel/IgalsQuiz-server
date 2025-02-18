package com.igalblech.igalsquizserver;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SocketMessage {

    private String id = "";
    private String type = "";
    private String source = "";
    private String name = "";
    private String jsonData = "";


    public SocketMessage()
    {
    }

    public SocketMessage(String id, String type, String source, String name)
    {
        super();
        this.id = id;
        this.type = type;
        this.source = source;
        this.name = name;
    }

    public SocketMessage(String id, String type, String source)
    {
        this(id, type, source, "");
    }

}
