package com.EarthQuake;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class EarthQuake {

    @Id
    private String eid;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date time;
    private double latitude;
    private double longitude;
    private double depth;
    private double mag;
    private String magType;
    private double nst;
    private double gap;
    private double dmin;
    private double rms;
    private String net;
    private String id;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date updated;
    private String place;
    private String type;
    private double horizontalError;
    private double depthError;
    private double magError;
    private double magNst;
    private String status;
    private String locationSource;
    private String magSource;

}
