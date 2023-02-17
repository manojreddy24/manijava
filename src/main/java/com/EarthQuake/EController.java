package com.EarthQuake;

import com.mongodb.client.model.geojson.Point;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;



import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;


@Controller
public class EController {

    @Autowired
    public EarthQuakeRepository earthQuakeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;


    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//        sdf.setTimeZone(TimeZone.getTimeZone("CST"));
        return sdf.format(date);
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("earthquakes", new ArrayList<>());
        model.addAttribute("input", new InputDto());
        return "main";
    }


    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   Model model) throws IOException, CsvException {

        Reader reader = new InputStreamReader(file.getInputStream());
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        List<EarthQuake> people = new ArrayList<>();
        people = csvReader.readAll().stream()
                .filter(Objects::nonNull)
                .map(x -> {
                            EarthQuake p = new EarthQuake();
                            try {

                                p.setTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(x[0]));
                                p.setLatitude(x[1].trim().isEmpty() ? 0 : Double.parseDouble(x[1].trim()));
                                p.setLongitude(x[2].trim().isEmpty() ? 0 : Double.parseDouble(x[2].trim()));
                                p.setDepth(x[3].trim().isEmpty() ? 0 : Double.parseDouble(x[3].trim()));
                                p.setMag(x[4].trim().isEmpty() ? 0 : Double.parseDouble(x[4].trim()));
                                p.setMagType(x[5]);
                                p.setNst(x[6].trim().isEmpty() ? 0 : Double.parseDouble(x[6].trim()));
                                p.setGap(x[7].trim().isEmpty() ? 0 : Double.parseDouble(x[7].trim()));
                                p.setDmin(x[8].trim().isEmpty() ? 0 : Double.parseDouble(x[8].trim()));
                                p.setRms(x[9].trim().isEmpty() ? 0 : Double.parseDouble(x[9].trim()));
                                p.setNet(x[10]);
                                p.setId(x[11]);
                                p.setUpdated(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(x[12]));
                                p.setPlace(x[13]);
                                p.setType(x[14]);
                                p.setHorizontalError(x[15].trim().isEmpty() ? 0 : Double.parseDouble(x[15].trim()));
                                p.setDepthError(x[16].trim().isEmpty() ? 0 : Double.parseDouble(x[16].trim()));
                                p.setMagError(x[17].trim().isEmpty() ? 0 : Double.parseDouble(x[17].trim()));
                                p.setMagNst(x[18].trim().isEmpty() ? 0 : Double.parseDouble(x[18].trim()));
                                p.setStatus(x[19]);
                                p.setLocationSource(x[20]);
                                p.setMagSource(x[21]);

                            } catch (Exception e) {
                                System.out.println(e.getMessage());

                            }
                            return p;
                        }
                ).collect(Collectors.toList());

        System.out.println("All processed");

        List<EarthQuake> list = earthQuakeRepository.saveAll(people);
      model.addAttribute("message", "Successfully Inserted records into the database..."+ list.size());
        model.addAttribute("list", list);

        return "table";
    }


    @GetMapping("/searchByMagnitude")
    public String searchByMagnitude(@ModelAttribute("input") InputDto input, Model model) {
        Criteria criteria = Criteria.where("mag").gte(input.getMag());
        Query query = new Query(criteria);
        List<EarthQuake> list = mongoTemplate.find(query, EarthQuake.class);
        model.addAttribute("message", "EarthQuakes with Magnitude greater than " + input.getMag() + " are: "+list.size());
        model.addAttribute("list", list);
        return "table";
    }

    @GetMapping("/searchBetweenDatesAndMagnitude")
    public String searchBetweenDatesAndMagnitude(@ModelAttribute("input") InputDto input, Model model) {
        Criteria criteria = new Criteria();
        criteria.andOperator(
                Criteria.where("time").gte(input.getDate1()),
                Criteria.where("time").lte(input.getDate2()),
                Criteria.where("mag").gte(input.getMag())
        );
        Query query = new Query(criteria);
        List<EarthQuake> list = mongoTemplate.find(query, EarthQuake.class);
        model.addAttribute("message", "EarthQuakes with Magnitude greater than "
                + input.getMag() + " between " + input.getDate1() + " and " + input.getDate2() + " are: "+list.size());
        model.addAttribute("list", list);
        return "table";
    }



    public double[] calculateDestinationPoint(double lat, double lon, double bearing, double distance) {
        double RADIUS = 6371;
        lat = Math.toRadians(lat);
        lon = Math.toRadians(lon);
        bearing = Math.toRadians(bearing);
        distance /= RADIUS;
        double destLat = Math.asin(Math.sin(lat) * Math.cos(distance) +
                Math.cos(lat) * Math.sin(distance) * Math.cos(bearing));
        double destLon = lon + Math.atan2(Math.sin(bearing) * Math.sin(distance) * Math.cos(lat),
                Math.cos(distance) - Math.sin(lat) * Math.sin(destLat));
        destLat = Math.toDegrees(destLat);
        destLon = Math.toDegrees(destLon);
        return new double[] {destLat, destLon};
    }

    @GetMapping("/searchWithInDistance")
    public String searchWithInDistance(@ModelAttribute("input") InputDto input, Model model) {
        double[] north = calculateDestinationPoint(input.getLatitude(), input.getLongitude(), 0.0, input.getMaxDistance());
        double[] south = calculateDestinationPoint(input.getLatitude(), input.getLongitude(), 180.0, input.getMaxDistance());
        double[] east = calculateDestinationPoint(input.getLatitude(), input.getLongitude(), 270.0, input.getMaxDistance());
        double[] west = calculateDestinationPoint(input.getLatitude(), input.getLongitude(), 90.0, input.getMaxDistance());

        //latitude is parallel
        // longitude is Straight
        Criteria criteria = new Criteria();

        criteria.andOperator(
                Criteria.where("latitude").lte(north[0]).gte(south[0]),
                Criteria.where("longitude").lte(west[1]).gte(east[1])
        );
        Query query = new Query(criteria);
        List<EarthQuake> list = mongoTemplate.find(query, EarthQuake.class);

        model.addAttribute("message", "EarthQuakes with in "
                + input.getMaxDistance() + "km from " + input.getLatitude() + " and "
                + input.getLongitude() + " are: "+list.size());
        model.addAttribute("list", list);
        return "table";

    }


    @GetMapping("/timeOfEarthQuake")
    public String test(@ModelAttribute("input") InputDto input, Model model) throws ParseException {
        Query query = new Query(Criteria.where("mag").gte(input.getMag()));
        List<EarthQuake> list = mongoTemplate.find(query, EarthQuake.class);

        SimpleDateFormat hourFormat = new SimpleDateFormat("HH");

        Map<String,Integer> map = new HashMap<>();

        for(EarthQuake e : list){
            String hour = hourFormat.format(e.getTime());
            map.put(hour.toString(),map.getOrDefault(hour.toString(),0)+1);
        }

        List<List<Object>> chartData = new ArrayList<>();
        map.entrySet().stream().forEach(entry -> {
            chartData.add(Arrays.asList(entry.getKey(),entry.getValue()));
        });
        model.addAttribute("chartData", chartData);
        model.addAttribute("message", "Total EarthQuakes with magnitude greater than " + input.getMag() + " are: " + list.size());
        Map.Entry<String, Integer> maxEntry = map.entrySet().stream().max(Map.Entry.comparingByValue()).get();
        Map.Entry<String, Integer> minEntry = map.entrySet().stream().min(Map.Entry.comparingByValue()).get();
        model.addAttribute("min", "Minimum time of EarthQuakes with magnitude greater than " + input.getMag() + " is: " + minEntry.getKey() + " at " + minEntry.getValue() + " times");
        model.addAttribute("max", "Maximum time of EarthQuakes with magnitude greater than " + input.getMag() + " is: " + maxEntry.getKey() + " at " + maxEntry.getValue() + " times");

        return "google-pie";
    }

    @GetMapping("deleteAll")
    public String deleteAll(Model model) {
        earthQuakeRepository.deleteAll();
        model.addAttribute("message", "All records deleted successfully");
        return "table";
    }
}
