package com.padshift.sonic.controller;

import com.ibm.watson.developer_cloud.assistant.v1.model.Intent;
import com.ibm.watson.developer_cloud.discovery.v1.Discovery;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;
import com.padshift.sonic.entities.*;
import com.padshift.sonic.service.GenreService;
import com.padshift.sonic.service.UserService;
import com.padshift.sonic.service.VideoService;
import org.hibernate.Session;
import org.hibernate.annotations.SourceType;
import org.hibernate.mapping.Array;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import radams.gracenote.webapi.GracenoteException;
import radams.gracenote.webapi.GracenoteMetadata;
import radams.gracenote.webapi.GracenoteWebAPI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.*;

/**
 * Created by ruzieljonm on 26/06/2018.
 */


@SuppressWarnings("Duplicates")
@Controller
@RequestMapping("/")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    VideoService videoService;

    @Autowired
    GenreService genreService;


    @RequestMapping("/sonic")
    public String showLoginPage(HttpSession session, Model model) {
        if(session.isNew()) {
            return "signinsignup";
        }else{
            return showHomepage(model,session);
        }
    }

    @RequestMapping(value = "/signin", method = RequestMethod.POST)
    public String generalSigninPost(HttpServletRequest request, Model model, HttpSession session) {
        String userName = request.getParameter("inputUserName1");
        String userPass = request.getParameter("inputPassword1");

        if(userName.equals("admin") && userPass.equals("admin")){
            return "HomePageAdmin";
        }else {

            User checkUser = userService.findByUsernameAndPassword(userName, userPass);

            if (checkUser != null) {
                session.setAttribute("userid", checkUser.getUserId());
                session.setAttribute("username", checkUser.getUserName());
                session.setAttribute("sessionid", checkUser.getUserId()+getSaltString());
                session.setAttribute("useragegroup", checkUser.getAgecriteriaId());
                System.out.println(checkUser.getUserId() + " " + checkUser.getUserName());

                return showHomepage(model, session);
            } else {
                return "signinsignup";
            }
        }
    }


    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public String generalSignup(HttpServletRequest request, Model model, HttpSession session) {
        User newUser = new User();
        int age;
        int ageGroup;
        newUser.setUserName(request.getParameter("inputUserName"));
        newUser.setUserPass(request.getParameter("inputPassword"));
        newUser.setUserEmail(request.getParameter("inputEmail"));

        Calendar now = Calendar.getInstance();   // Gets the current date and time
        int year = now.get(Calendar.YEAR);       // The current year

        System.out.println("BIRTHDAY CYST : " + request.getParameter("bday"));
        System.out.println("TYPE CYST : " + request.getParameter("radio"));

        String bday = request.getParameter("bday");
        String upToNCharacters = bday.substring(0, Math.min(bday.length(), 4));
        System.out.println(upToNCharacters);

        age = year-Integer.parseInt(upToNCharacters);
        newUser.setUserAge(year-Integer.parseInt(upToNCharacters));
        newUser.setUserPersonality(request.getParameter("radio"));

        if(age<=24){
            ageGroup = 1;
        }else if(age >=25 && age<=34){
            ageGroup = 2;
        }else if(age >=35 && age <=44) {
            ageGroup = 3;
        }else if(age >=45 && age <=54) {
            ageGroup = 4;
        }else if(age >=55 && age <=64) {
            ageGroup = 5;
        }else{
            ageGroup = 6;
        }
        newUser.setAgecriteriaId(ageGroup);

        userService.saveUser(newUser);
        User checkUser = userService.findByUsername(request.getParameter("inputUserName"));
        session.setAttribute("userid", checkUser.getUserId());
        session.setAttribute("username", request.getParameter("inputUserName"));
        session.setAttribute("sessionid", checkUser.getUserId()+getSaltString());
        session.setAttribute("useragegroup", checkUser.getAgecriteriaId());
        return showGenreSelection(model,session);
    }

    protected String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 5) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }


    @RequestMapping("/genreselection")
    public String showGenreSelection(Model model, HttpSession session){

        ArrayList<Genre> genres = genreService.findAll();

        for (int i=0; i<genres.size(); i++){
            System.out.println(genres.get(i).getGenreName());
        }
        model.addAttribute("genres", genres);
        model.addAttribute("starname", "pota");

        return "GenreSelection";
    }

    @RequestMapping(value = "/submitpref", method = RequestMethod.POST)
    public String submitPreference(HttpServletRequest request, HttpSession session, Model model) {

        System.out.println(session.getAttribute("userid") + "usah id");

        ArrayList<Genre> genres = genreService.findAll();
        UserPreference[] genrePreference = new UserPreference[genres.size()];

        for (int i=0; i<genrePreference.length; i++){
            genrePreference[i] = new UserPreference();
        }
        int userid = Integer.parseInt(session.getAttribute("userid").toString());
        String username = (String) session.getAttribute("username");

        for (int i=0; i<genrePreference.length; i++){
            genrePreference[i].setUserId(userid);
            float temp;
            if(request.getParameter(genres.get(i).getGenreName().toString())==null){
                temp=0;
            }else {
                temp = Float.parseFloat(request.getParameter(genres.get(i).getGenreName().toString()));
            }
            genrePreference[i].setPrefWeight(temp);
            genrePreference[i].setGenreId(genres.get(i).getGenreId());
            genrePreference[i].setUserName(username);
            genrePreference[i].setGenreName(genres.get(i).getGenreName());
            System.out.println(genrePreference[i].getUserId() + "-" + genrePreference[i].getGenreId() + "-" + genrePreference[i].getPrefWeight());

            userService.saveUserPreference(genrePreference[i]);

            updategenreWeight(genres.get(i).getGenreId(), userid, temp);
        }

        return showHomepage(model,session);

    }

    public void updategenreWeight(int genreid, int userid, float temp){
        User user = userService.findByUserId(userid);

        int agegroup = user.getAgecriteriaId();
        AgeCriteria useragegroup = userService.findByAgeCriteriaId(agegroup);
        int totalviews = useragegroup.getAlternativeMusic() + useragegroup.getCountryMusic() + useragegroup.getHiphopMusic() + useragegroup.getHouseMusic() + useragegroup.getPopMusic() + useragegroup.getReggaeMusic() + useragegroup.getReligiousMusic() + useragegroup.getRnbMusic() + useragegroup.getRockMusic();

        float userInput = temp;
        float genreAgePop,genreAgeRock, genreAgeAlt, genreAgeRBS, genreAgeCntry, genreAgeHouse, genreAgeReg, genreAgeRel, genreAgeHH;
        float genweight = 0;

        float genrePTPop,genrePTRock, genrePTAlt, genrePTRBS, genrePTCntry, genrePTHouse, genrePTReg, genrePTRel, genrePTHH;


        if(user.getUserPersonality().equals("introvert")){
            genrePTRock = (float) 10.0;
            genrePTAlt = (float) 10.0;
            genrePTReg = (float) 10.0;
            genrePTRel = (float) 10.0;

            genrePTPop = (float) 5.0;
            genrePTRBS = (float) 5.0;
            genrePTCntry = (float) 5.0;
            genrePTHouse = (float) 5.0;
            genrePTHH = (float) 5.0;
        }else{

            genrePTRock = (float) 5.0;
            genrePTAlt = (float) 5.0;
            genrePTReg = (float) 5.0;
            genrePTRel = (float) 5.0;

            genrePTPop = (float) 10.0;
            genrePTRBS = (float) 10.0;
            genrePTCntry = (float) 10.0;
            genrePTHouse = (float) 10.0;
            genrePTHH = (float) 10.0;

        }

        UserPreference userpref = userService.findUserPreferenceByUserIdAndGenreId(userid, genreid);

        float genreAge = 0;
        float genrePT =0;

        if(genreid == 1 && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),1).getPrefWeight();
            genreAge = useragegroup.getPopMusic()/totalviews;
            genrePT = genrePTPop;
        }
        if(genreid == 2 && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),2).getPrefWeight();
            genreAge = useragegroup.getRockMusic()/totalviews;
            genrePT = genrePTRock;
        }

        if(genreid == 3  && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),3).getPrefWeight();
            genreAge = useragegroup.getAlternativeMusic()/totalviews;
            genrePT = genrePTAlt;
        }
        if(genreid == 4  && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),4).getPrefWeight();
            genreAge = useragegroup.getRnbMusic()/totalviews;
            genrePT = genrePTRBS;
        }
        if(genreid == 5  && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),5).getPrefWeight();
            genreAge = useragegroup.getCountryMusic()/totalviews;
            genrePT = genrePTCntry;
        }
        if(genreid == 6  && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),6).getPrefWeight();
            genreAge = useragegroup.getCountryMusic()/totalviews;
            genrePT = genrePTHouse;
        }
        if(genreid == 7  && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),7).getPrefWeight();
            genreAge = useragegroup.getReggaeMusic()/totalviews;
            genrePT = genrePTReg;
        }
        if(genreid == 8  && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),8).getPrefWeight();
            genreAge = useragegroup.getReligiousMusic()/totalviews;
            genrePT = genrePTRel;
        }
        if(genreid == 9  && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),9).getPrefWeight();
            genreAge = useragegroup.getHiphopMusic()/totalviews;
            genrePT = genrePTHH;
        }

        if(totalviews == 9){
            genreAge = 1;
        }

        float uipercent, agepercent,pertypepercent;
        Criteria ui = userService.findCriteriaByCriteriaName("userinput");
        System.out.println("++++++ LOOL - "+ ui.toString());
        if(ui.toString()!=null){
            uipercent=ui.getCriteriaPercentage();
        }else{
            uipercent=0;
        }

        Criteria age = userService.findCriteriaByCriteriaName("age");
        if(age!=null){
            agepercent = age.getCriteriaPercentage();
        }else{
            agepercent=0;
        }

        Criteria pertype = userService.findCriteriaByCriteriaName("personality");
        if(pertype!=null){
            pertypepercent = pertype.getCriteriaPercentage();
        }else{
            pertypepercent=0;
        }

        genweight = (((temp/10)*uipercent)+((genreAge/10)*agepercent)+((genrePT/10)*pertypepercent));

        userpref.setGenreWeight(genweight);
        userService.saveUserPreference(userpref);
    }

    String topgenre=null;

    @RequestMapping("/homepagev2")
    public String showHomepage(Model model, HttpSession session) {
        updateagecriteria();
        String userid = session.getAttribute("userid").toString();
        System.out.println("tang ina" + userid);
        User user = userService.findByUserId(Integer.parseInt(userid));

        System.out.println(user.getUserId() + " "+ user.getUserName());


        ArrayList<VideoDetails> videos = videoService.findAllVideoDetails();
        ArrayList<RecVid> recVideos = new ArrayList<>();

        for( VideoDetails vid : videos){
            RecVid rv = new RecVid();
            rv.setVideoid(vid.getVideoid());
            rv.setTitle(vid.getTitle());
            rv.setArtist(vid.getArtist());
            rv.setGenre(vid.getGenre());
            rv.setViewCount(vid.getViewCount());

            rv.setWeight(computeInitialVideoWeight(vid,user));
            recVideos.add(rv);
        }

        Collections.sort(recVideos);


        for(int i=0; i<10; i++){
            System.out.println(recVideos.get(i).getTitle() + " : " + recVideos.get(i).getWeight());
        }

        ArrayList<RecVid> vr1 = new ArrayList<RecVid>();
        ArrayList<RecVid> vr2 = new ArrayList<RecVid>();
        ArrayList<RecVid> vr3 = new ArrayList<RecVid>();
        ArrayList<RecVid> vr4 = new ArrayList<RecVid>();

        for(int i=0; i<=5; i++) {
            RecVid vid1 = new RecVid(recVideos.get(i).getVideoid(), recVideos.get(i).getTitle(), recVideos.get(i).getArtist(), recVideos.get(i).getGenre(), "https://i.ytimg.com/vi/" + recVideos.get(i).getVideoid() + "/mqdefault.jpg");
            vr1.add(vid1);
            vid1 = null;
        }

        for(int i=6; i<=11; i++) {
            RecVid vid2 = new RecVid(recVideos.get(i).getVideoid(), recVideos.get(i).getTitle(), recVideos.get(i).getArtist(), recVideos.get(i).getGenre(), "https://i.ytimg.com/vi/" + recVideos.get(i).getVideoid() + "/mqdefault.jpg");
            vr2.add(vid2);
            vid2 = null;
        }

        for(int i=12; i<=17; i++) {
            RecVid vid3 = new RecVid(recVideos.get(i).getVideoid(), recVideos.get(i).getTitle(), recVideos.get(i).getArtist(), recVideos.get(i).getGenre(), "https://i.ytimg.com/vi/" + recVideos.get(i).getVideoid() + "/mqdefault.jpg");
            vr3.add(vid3);
            vid3 = null;
        }


        for(int i=18; i<=23; i++) {
            RecVid vid4 = new RecVid(recVideos.get(i).getVideoid(), recVideos.get(i).getTitle(), recVideos.get(i).getArtist(), recVideos.get(i).getGenre(), "https://i.ytimg.com/vi/" + recVideos.get(i).getVideoid() + "/mqdefault.jpg");
            vr4.add(vid4);
            vid4 = null;


        }

        findsimilarUsers(session.getAttribute("userid").toString(), session);

        String[] users = (String[]) session.getAttribute("similarusers");
        System.out.println("SIMILAR USERS");
        for (int i = 0; i < users.length; i++) {
            System.out.println(users[i]+"======");
        }
        model.addAttribute("r1", vr1);
        model.addAttribute("r2", vr2);
        model.addAttribute("r3", vr3);
        model.addAttribute("r4", vr4);


        return "Homepage";


//
//        ArrayList<UserPreference> userPref = userService.findAllByUserId(user.getUserId());
//
//        for(UserPreference usp : userPref){
//            System.out.println(usp.getGenreId() + " : " + usp.getPrefWeight());
//        }
//
//        Collections.sort(userPref, UserPreference.PrefWeightComparator);
//        System.out.println("sorted");
//        for(UserPreference usp : userPref){
//            System.out.println(usp.getGenreId() + " : " + usp.getPrefWeight());
//        }
//
//
//        ArrayList<Genre> genres = videoService.findAllGenre();
//
//        for(Genre gen : genres){
//            System.out.println(gen.getGenreName());
//        }
//
//
//        ArrayList<VVD> vr1 = new ArrayList<VVD>();
//        ArrayList<VVD> vr2 = new ArrayList<VVD>();
//        ArrayList<VVD> vr3 = new ArrayList<VVD>();
//        ArrayList<VVD> vr4 = new ArrayList<VVD>();
//
//
//        String[] topfour = new String[4];
//
//        for(int i=0; i<topfour.length; i++){
//            topfour[i] = (videoService.findGenreByGenreId(userPref.get(i).getGenreId())).getGenreName();
//        }
//
//        topgenre = topfour[0];
//
//
//
//        ArrayList<VideoDetails> vidListGen1 = videoService.findAllByGenre(topfour[0]);
//        ArrayList<VideoDetails> vidListGen2 = videoService.findAllByGenre(topfour[1]);
//        ArrayList<VideoDetails> vidListGen3 = videoService.findAllByGenre(topfour[2]);
//        ArrayList<VideoDetails> vidListGen4 = videoService.findAllByGenre(topfour[3]);
//
//        for(int i=0; i<5; i++){
//            System.out.println(vidListGen1.get(i).getTitle() +" ------------- " + vidListGen1.get(i).getViewCount());
//        }
//
//        System.out.println("----------IMO MAMA GA SORTING------------");
//
//        Collections.sort(vidListGen1);
//        Collections.sort(vidListGen2);
//        Collections.sort(vidListGen3);
//        Collections.sort(vidListGen4);
//
//        for(int i=0; i<5; i++){
//            System.out.println(vidListGen1.get(i).getTitle() +" ------------- " + vidListGen1.get(i).getViewCount());
//        }
//
//
//        for(int i=0; i<6; i++){
//            VVD vid1 = new VVD(vidListGen1.get(i).getVideoid(), vidListGen1.get(i).getTitle(), vidListGen1.get(i).getArtist(), vidListGen1.get(i).getGenre(), vidListGen1.get(i).getDate(), "https://i.ytimg.com/vi/" + vidListGen1.get(i).getVideoid() + "/mqdefault.jpg");
//            vr1.add(vid1);
//            vid1 = null;
//
//            VVD vid2 = new VVD(vidListGen2.get(i).getVideoid(), vidListGen2.get(i).getTitle(), vidListGen2.get(i).getArtist(), vidListGen2.get(i).getGenre(), vidListGen2.get(i).getDate(), "https://i.ytimg.com/vi/" + vidListGen2.get(i).getVideoid() + "/mqdefault.jpg");
//            vr2.add(vid2);
//            vid2 = null;
//
//            VVD vid3 = new VVD(vidListGen3.get(i).getVideoid(), vidListGen3.get(i).getTitle(), vidListGen3.get(i).getArtist(), vidListGen3.get(i).getGenre(), vidListGen3.get(i).getDate(), "https://i.ytimg.com/vi/" + vidListGen3.get(i).getVideoid() + "/mqdefault.jpg");
//            vr3.add(vid3);
//            vid3 = null;
//
//            VVD vid4 = new VVD(vidListGen4.get(i).getVideoid(), vidListGen4.get(i).getTitle(), vidListGen4.get(i).getArtist(), vidListGen4.get(i).getGenre(), vidListGen4.get(i).getDate(), "https://i.ytimg.com/vi/" + vidListGen4.get(i).getVideoid() + "/mqdefault.jpg");
//            vr4.add(vid4);
//            vid4 = null;
//
//
//        }
//
//        model.addAttribute("r1", vr1);
//        model.addAttribute("r2", vr2);
//        model.addAttribute("r3", vr3);
//        model.addAttribute("r4", vr4);
//        return "Homepage";

    }

    public void updateagecriteria(){
        try{
            for (int i = 0; i < 6; i++) {
                AgeCriteria age = userService.findByAgeCriteriaId(i);
            }
        }catch (Exception e){
            ArrayList<Integer> agegroups = userService.findDistinctAgeGroup();

            for (int i = 0; i < agegroups.size(); i++) {
                boolean group1 = agegroups.get(i).equals(1);
                boolean group2 = agegroups.get(i).equals(2);
                boolean group3 = agegroups.get(i).equals(3);
                boolean group4 = agegroups.get(i).equals(4);
                boolean group5 = agegroups.get(i).equals(5);
                boolean group6 = agegroups.get(i).equals(6);

                if(group1 == true){
                    AgeCriteria agecriteria = new AgeCriteria();
                    agecriteria.setAgeGroup("Age Group 1");
                    agecriteria.setAgecriteriaId(1);
                    userService.saveAgeCriteria(agecriteria);
                    agecriteria = null;
                }

                if(group2 == true){
                    AgeCriteria agecriteria = new AgeCriteria();
                    agecriteria.setAgeGroup("Age Group 2");
                    agecriteria.setAgecriteriaId(2);
                    userService.saveAgeCriteria(agecriteria);
                    agecriteria = null;
                }

                if(group3 == true){
                    AgeCriteria agecriteria = new AgeCriteria();
                    agecriteria.setAgeGroup("Age Group 3");
                    agecriteria.setAgecriteriaId(3);
                    userService.saveAgeCriteria(agecriteria);
                    agecriteria = null;
                }

                if(group4 == true){
                    AgeCriteria agecriteria = new AgeCriteria();
                    agecriteria.setAgeGroup("Age Group 4");
                    agecriteria.setAgecriteriaId(4);
                    userService.saveAgeCriteria(agecriteria);
                    agecriteria = null;
                }

                if(group5 == true){
                    AgeCriteria agecriteria = new AgeCriteria();
                    agecriteria.setAgeGroup("Age Group 5");
                    agecriteria.setAgecriteriaId(5);
                    userService.saveAgeCriteria(agecriteria);
                    agecriteria = null;
                }

                if(group6 == true){
                    AgeCriteria agecriteria = new AgeCriteria();
                    agecriteria.setAgeGroup("Age Group 6");
                    agecriteria.setAgecriteriaId(6);
                    userService.saveAgeCriteria(agecriteria);
                    agecriteria = null;
                }
            }
        }
    }

    public void findsimilarUsers(String currentuserId, HttpSession session){
        ArrayList<String> allusers = new ArrayList<>();
//        ArrayList<User> users = userService.findOtherUser(currentuser);
        ArrayList<Integer> users = userService.findDistinctUserfromUserPref(Integer.parseInt(currentuserId));
        ArrayList<Integer> genreIds = userService.findDistinctGenre();
        ArrayList<Float> genreweights = new ArrayList<>();
        float gweight = 0;
        int current = userService.findUserIdByUserId(Integer.parseInt(currentuserId));
        String currentU = String.valueOf(current);
        System.out.println(currentU);
        allusers.add(currentU);

        for (int i=0; i < users.size(); i++){
            allusers.add(users.get(i).toString());
        }

        for (int i = 0; i < allusers.size(); i++) {
            System.out.println(allusers.get(i));
        }

        for (int i = 0; i < genreIds.size(); i++) {
            System.out.printf("%15s", genreIds.get(i));
        }
        System.out.println();
        for (int i = 0; i < allusers.size(); i++) {
            System.out.println(allusers.get(i));
            for (int j = 0; j < genreIds.size(); j++) {
                UserPreference genweight = userService.findUserPreferenceByUserIdAndGenreId(Integer.parseInt(allusers.get(i)), genreIds.get(j));
                gweight = genweight.getGenreWeight();
                System.out.println("-----"+allusers.get(i)+" xx "+genreIds.get(j)+" xx "+ gweight);
//                if(gweight == null){
//                    gweight = 0;
//                }
                genreweights.add(gweight);
            }
        }

        float[] currentrow = new float[genreIds.size()];
        float[] otherrow = new float[genreweights.size()];

        int count = 0;

        for (int i = 0; i < genreIds.size(); i++) {
            System.out.printf("%15s", genreIds.get(i));
        }
        System.out.println();
        for (int i = 0; i < allusers.size(); i++) {
            System.out.printf("%s", allusers.get(i));
            for (int j = 0; j < genreIds.size(); j++) {
                System.out.printf("%15f", genreweights.get(count));
                if( i == 0 ){
                    currentrow[j] = genreweights.get(count);
                }
                else{
                    otherrow[count] = genreweights.get(count);
                }
                count++;
            }
            System.out.println();
        }
        float[] distancevalue = new float[allusers.size()];
        float[] otheruserrow = new float[genreweights.size()];
        count = genreIds.size();
        for (int i = 1; i < allusers.size(); i++) {
            for (int j = 0; j < genreIds.size(); j++) {
                otheruserrow[j] = otherrow[count];
                count++;
            }
            distancevalue[i] =finddistanceValue(currentrow, otheruserrow, currentuserId, allusers.get(i), genreIds);
        }
        String[] arrUser = new String[allusers.size()];
        for (int i = 0; i < arrUser.length; i++) {
            arrUser[i] = allusers.get(i);
        }
        float tempGenweight = 0;
        String tempuser;
        ArrayList<String> similarUsers = new ArrayList<>();
        for (int i = 0; i < allusers.size(); i++) {
            for (int j = i+1; j < arrUser.length; j++) {
                if (distancevalue[i] < distancevalue[j])
                {
                    tempGenweight = distancevalue[i];
                    tempuser = arrUser[i];
                    distancevalue[i] = distancevalue[j];
                    arrUser[i] = arrUser[j];
                    distancevalue[j] = tempGenweight;
                    arrUser[j] = tempuser;
                }
            }
        }

        String[] twosimusers = new String[2];
        float[] twodistancevalue = new float[2];

        for (int i = 0; i < 2; i++) {
            twodistancevalue[i] = distancevalue[i];
            twosimusers[i] = arrUser[i];
        }

        for (int i = 0; i < distancevalue.length; i++) {
            System.out.println(arrUser[i]+"xxxx"+distancevalue[i]);
        }
        session.setAttribute("similarusers", twosimusers);
        session.setAttribute("distancevalue", twodistancevalue);
    }

    public float finddistanceValue(float[] currentuserGenweight, float[] otheruserGenweight, String currentuser, String otheruser, ArrayList<Integer> genIds){
        float genWeight = 0;
        float sum = 0, sub1, square;
        for (int i = 0; i < genIds.size(); i++) {
            System.out.printf("%15s", genIds.get(i));
        }
        System.out.println();
        System.out.printf("%s", currentuser);
        for (int i = 0; i < currentuserGenweight.length; i++) {
            System.out.printf("%15f", currentuserGenweight[i]);
        }
        System.out.println();
        System.out.printf("%s", otheruser);
        for (int i = 0; i < genIds.size(); i++) {
            System.out.printf("%15f", otheruserGenweight[i]);
        }
        System.out.println();
        for (int i = 0; i < genIds.size(); i++) {
            sub1 = currentuserGenweight[i] - otheruserGenweight[i];
            sum += sub1 * sub1;
        }
        System.out.println(Math.sqrt(sum));
        float ret = (float) Math.sqrt(sum);

        return ret;
    }

    public float computeInitialVideoWeight(VideoDetails video, User user){
        System.out.println(video.getTitle()+"==========="+video.getLikes());
        int agegroup = user.getAgecriteriaId();
        AgeCriteria useragegroup = userService.findByAgeCriteriaId(agegroup);
        float totalviews = useragegroup.getAlternativeMusic() + useragegroup.getCountryMusic() + useragegroup.getHiphopMusic() + useragegroup.getHouseMusic() + useragegroup.getPopMusic() + useragegroup.getReggaeMusic() + useragegroup.getReligiousMusic() + useragegroup.getRnbMusic() + useragegroup.getRockMusic();
        float vidWeight;
        float userInput = 0;

        float genreAgePop,genreAgeRock, genreAgeAlt, genreAgeRBS, genreAgeCntry, genreAgeHouse, genreAgeReg, genreAgeRel, genreAgeHH;

        float genrePTPop,genrePTRock, genrePTAlt, genrePTRBS, genrePTCntry, genrePTHouse, genrePTReg, genrePTRel, genrePTHH;


        if(user.getUserPersonality().equals("introvert")){
            genrePTRock = (float) 10.0;
            genrePTAlt = (float) 10.0;
            genrePTReg = (float) 10.0;
            genrePTRel = (float) 10.0;

            genrePTPop = (float) 5.0;
            genrePTRBS = (float) 5.0;
            genrePTCntry = (float) 5.0;
            genrePTHouse = (float) 5.0;
            genrePTHH = (float) 5.0;
        }else{

            genrePTRock = (float) 5.0;
            genrePTAlt = (float) 5.0;
            genrePTReg = (float) 5.0;
            genrePTRel = (float) 5.0;

            genrePTPop = (float) 10.0;
            genrePTRBS = (float) 10.0;
            genrePTCntry = (float) 10.0;
            genrePTHouse = (float) 10.0;
            genrePTHH = (float) 10.0;

        }

        ArrayList<UserPreference> userPref = userService.findAllGenrePreferenceByUserId(user.getUserId());

        float genreAge = 0;
        float genrePT =0;
        int genreid = 0;
//
//        userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),1);
//        System.out.println("prefweight:" + userInput.getPrefWeight());
//
//        float upPop, upRock, upAlt, upRB, upCntry, upHouse, upReg, upRel, upHH;
//        upPop = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),1).getPrefWeight();
//

        if(video.getGenre().equals("Pop Music") && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),1).getPrefWeight();
            genreid = 1;
            genreAge = useragegroup.getPopMusic()/totalviews;
            genrePT = genrePTPop;
        }
        if(video.getGenre().equals("Rock Music") && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),2).getPrefWeight();
            genreid = 2;
            genreAge = useragegroup.getRockMusic()/totalviews;
            genrePT = genrePTRock;
        }

        if(video.getGenre().equals("Alternative Music") && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),3).getPrefWeight();
            genreid = 3;
            genreAge = useragegroup.getAlternativeMusic()/totalviews;
            genrePT = genrePTAlt;
        }
        if(video.getGenre().equals("R&B/Soul Music") && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),4).getPrefWeight();
            genreid = 4;
            genreAge = useragegroup.getRnbMusic()/totalviews;
            genrePT = genrePTRBS;
        }
        if(video.getGenre().equals("Country Music") && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),5).getPrefWeight();
            genreid = 5;
            genreAge = useragegroup.getCountryMusic()/totalviews;
            genrePT = genrePTCntry;
        }
        if(video.getGenre().equals("House Music") && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),6).getPrefWeight();
            genreid = 6;
            genreAge = useragegroup.getCountryMusic()/totalviews;
            genrePT = genrePTHouse;
        }
        if(video.getGenre().equals("Reggae Music") && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),7).getPrefWeight();
            genreid = 7;
            genreAge = useragegroup.getReggaeMusic()/totalviews;
            genrePT = genrePTReg;
        }
        if(video.getGenre().equals("Religious Music") && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),8).getPrefWeight();
            genreid = 8;
            genreAge = useragegroup.getReligiousMusic()/totalviews;
            genrePT = genrePTRel;
        }
        if(video.getGenre().equals("Hip-Hop/Rap Music") && totalviews != 9){
            userInput = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(),9).getPrefWeight();
            genreid = 9;
            genreAge = useragegroup.getHiphopMusic()/totalviews;
            genrePT = genrePTHH;
        }

        if(totalviews == 9){
            genreAge = 1;
        }

        float uipercent, agepercent,pertypepercent;
        Criteria ui = userService.findCriteriaByCriteriaName("userinput");
        System.out.println("++++++ LOOL - "+ ui.toString());
        if(ui.toString()!=null){
            uipercent=ui.getCriteriaPercentage();
        }else{
            uipercent=0;
        }

        Criteria age = userService.findCriteriaByCriteriaName("age");
        if(age!=null){
            agepercent = age.getCriteriaPercentage();
        }else{
            agepercent=0;
        }

        Criteria pertype = userService.findCriteriaByCriteriaName("personality");
        if(pertype!=null){
            pertypepercent = pertype.getCriteriaPercentage();
        }else{
            pertypepercent=0;
        }
        float likes;
        float views = Float.parseFloat(video.getViewCount().toString());
        if(video.getLikes().equals("0")){
             likes =1;
        }else{
             likes = Float.parseFloat(video.getLikes().toString());
        }
        System.out.println("Total: "+totalviews);
        System.out.println("UI PERCENTAGE : " + uipercent);
        System.out.println("UI PERCENTAGE : " + agepercent);
        System.out.println("UI PERCENTAGE : " + pertypepercent);
        System.out.println(userInput);
        System.out.println(genreAge);
        System.out.println(genrePT);

        float genreweight = ((userInput/10)*uipercent)+((genreAge/10)*agepercent)+((genrePT/10)*pertypepercent);
        vidWeight= (float) ((((userInput/10)*uipercent)+((genreAge/10)*agepercent)+((genrePT/10)*pertypepercent))*likes);
        System.out.println("VID WEIGHT : " + vidWeight);

        UserPreference userpref = userService.findUserPreferenceByUserIdAndGenreId(user.getUserId(), genreid);
        userpref.setGenreWeight(genreweight);
        userService.saveUserPreference(userpref);
        return vidWeight;
    }



    @RequestMapping("/gotoPlayer")
    public String gotoPlayer(HttpServletRequest request, Model model, HttpSession session){
        String vididtoplay = request.getParameter("clicked");
        session.setAttribute("vididtoplay", vididtoplay);

        System.out.println("video id : " + vididtoplay);
        System.out.println("aaaaaaaaaaa" + session.getAttribute("userid"));

        UserHistory userhist = new UserHistory();
        userhist.setUserId(session.getAttribute("userid").toString());
        userhist.setVideoid(vididtoplay);
        userhist.setSeqid(session.getAttribute("sessionid").toString());
        userhist.setUserName(session.getAttribute("username").toString());
        userhist.setVidRating("0");
        userService.saveUserHistory(userhist);


        VideoDetails playvid = videoService.findByVideoid(vididtoplay);
        ArrayList<VideoDetails> upnext = (ArrayList<VideoDetails>) videoService.findAllVideoDetails();
//        Collections.sort(upnext);
        Collections.shuffle(upnext);
        System.out.println(playvid.getTitle() + " " + playvid.getArtist());

        String url = "https://www.youtube.com/embed/" + playvid.getVideoid();

        String thumbnail1 = "https://i.ytimg.com/vi/" + upnext.get(1).getVideoid() +"/mqdefault.jpg";
        String thumbnail2 = "https://i.ytimg.com/vi/" + upnext.get(2).getVideoid() +"/mqdefault.jpg";
        String thumbnail3 = "https://i.ytimg.com/vi/" + upnext.get(3).getVideoid() +"/mqdefault.jpg";

        ArrayList<VideoDetails> videoList = new ArrayList<VideoDetails>();
        videoList = videoService.findAllVideoDetails();

        ArrayList<VVD> vr1 = new ArrayList<VVD>();

        for (int i = 0; i < 6; i++) {
            VVD vid = new VVD(videoList.get(i).getVideoid(), videoList.get(i).getTitle(), videoList.get(i).getArtist(), videoList.get(i).getGenre(), videoList.get(i).getDate(),"https://i.ytimg.com/vi/" + videoList.get(i).getVideoid() + "/mqdefault.jpg");
            vr1.add(vid);
            vid = null;
        }

        model.addAttribute("r1", vr1);

        model.addAttribute("emblink", url);
        model.addAttribute("vidtitle", playvid.getTitle());
        model.addAttribute("vidviews", concat(playvid.getViewCount()));
        model.addAttribute("vidlikes", concat(playvid.getLikes()));

//        model.addAttribute("upnext")
        model.addAttribute("upnext1", upnext.get(1));
        model.addAttribute("upnext2", upnext.get(2));
        model.addAttribute("upnext3", upnext.get(3));
        model.addAttribute("vidid", vididtoplay);

        model.addAttribute("tn1", thumbnail1);
        model.addAttribute("tn2", thumbnail2);
        model.addAttribute("tn3", thumbnail3);

        return "VideoPlayerV2";

    }

    @RequestMapping("/submitrating")
    public String submitRating(HttpServletRequest request, Model model, HttpSession session){
        int useragegroup = (Integer) session.getAttribute("useragegroup");
        String vididtoplay = request.getParameter("current");
        String vididnexttoplay = request.getParameter("clicked");
        String vidrating = request.getParameter("rating");

        VideoDetails video = videoService.findByVideoid(vididtoplay);
        incrementagegroup(useragegroup, video.getGenre());

        List<UserHistory> currentuserhist = videoService.findAllByUserIdandVideoid(session.getAttribute("userid").toString(), vididtoplay);

        try{
            VidRatings uservideorating = videoService.findVidRatByUserIdandVideoid(session.getAttribute("userid").toString(), vididtoplay);
            if(!vidrating.equals("0")){
                uservideorating.setRating(vidrating);
                videoService.saveVidrating(uservideorating);
            }
        }catch (Exception e){
            VidRatings newrating = new VidRatings();
            newrating.setUserId(session.getAttribute("userid").toString());
            newrating.setUserName(session.getAttribute("username").toString());
            newrating.setVideoid(vididtoplay);
            newrating.setRating(vidrating);
            videoService.saveVidrating(newrating);
        }


        for (int i = 0; i < currentuserhist.size(); i++) {
            System.out.println(currentuserhist.get(i).getUserName()+"-----"+currentuserhist.get(i).getVideoid()+"-----"+vidrating);
            currentuserhist.get(i).setVidRating(vidrating);
            userService.saveUserHistory(currentuserhist.get(i));
        }
        VideoDetails playvid = videoService.findByVideoid(vididnexttoplay);
        ArrayList<VideoDetails> upnext = (ArrayList<VideoDetails>) videoService.findAllVideoDetails();
//        Collections.sort(upnext);
        Collections.shuffle(upnext);
//        System.out.println(playvid.getTitle() + " " + playvid.getArtist());

        String url = "https://www.youtube.com/embed/" + playvid.getVideoid();

        String thumbnail1 = "https://i.ytimg.com/vi/" + upnext.get(1).getVideoid() +"/mqdefault.jpg";
        String thumbnail2 = "https://i.ytimg.com/vi/" + upnext.get(2).getVideoid() +"/mqdefault.jpg";
        String thumbnail3 = "https://i.ytimg.com/vi/" + upnext.get(3).getVideoid() +"/mqdefault.jpg";

        ArrayList<VideoDetails> videoList = new ArrayList<VideoDetails>();
        videoList = videoService.findAllVideoDetails();
        VideoDetails recommVids = new VideoDetails();

        ArrayList<VVD> vr1 = new ArrayList<VVD>();

        String[] recommendedVids = cosineMatrix(session.getAttribute("userid").toString());
        System.out.println(recommendedVids[0]);
        int j =0;
        for (int i = 0; i < 6; i++) {
            if(recommendedVids[j] != null && i < recommendedVids.length){
                recommVids = videoService.findByVideoid(recommendedVids[i]);
                VVD vid = new VVD(recommVids.getVideoid(), recommVids.getTitle(), recommVids.getArtist(), recommVids.getGenre(), recommVids.getDate(),"https://i.ytimg.com/vi/" + recommVids.getVideoid() + "/mqdefault.jpg");
                vr1.add(vid);
                vid = null;
                j++;
            }
            else{
                VVD vid = new VVD(videoList.get(i).getVideoid(), videoList.get(i).getTitle(), videoList.get(i).getArtist(), videoList.get(i).getGenre(), videoList.get(i).getDate(),"https://i.ytimg.com/vi/" + videoList.get(i).getVideoid() + "/mqdefault.jpg");
                vr1.add(vid);
                vid = null;
            }
        }

//        for (int i = 0; i < 6; i++) {
//            VVD vid = new VVD(videoList.get(i).getVideoid(), videoList.get(i).getTitle(), videoList.get(i).getArtist(), videoList.get(i).getGenre(), videoList.get(i).getDate(),"https://i.ytimg.com/vi/" + videoList.get(i).getVideoid() + "/mqdefault.jpg");
//            vr1.add(vid);
//            vid = null;
//        }

        model.addAttribute("r1", vr1);

        model.addAttribute("emblink", url);
        model.addAttribute("vidtitle", playvid.getTitle());
        model.addAttribute("vidviews", concat(playvid.getViewCount()));
        model.addAttribute("vidlikes", concat(playvid.getLikes()));
        model.addAttribute("vidid", vididnexttoplay);

//        model.addAttribute("upnext")
        model.addAttribute("upnext1", upnext.get(1));
        model.addAttribute("upnext2", upnext.get(2));
        model.addAttribute("upnext3", upnext.get(3));

        model.addAttribute("tn1", thumbnail1);
        model.addAttribute("tn2", thumbnail2);
        model.addAttribute("tn3", thumbnail3);

        return "VideoPlayerV2";
    }

    public void incrementagegroup(int agegroup, String genre){
        AgeCriteria agecriteria = userService.findByAgeCriteriaId(agegroup);
        if(genre.contains("Pop")){
            agecriteria.setPopMusic(agecriteria.getPopMusic()+1);
        }
        if(genre.contains("House")){
            agecriteria.setPopMusic(agecriteria.getHouseMusic()+1);
        }
        if(genre.contains("Alternative")){
            agecriteria.setPopMusic(agecriteria.getAlternativeMusic()+1);
        }
        if(genre.contains("Reggae")){
            agecriteria.setPopMusic(agecriteria.getReggaeMusic()+1);
        }
        if(genre.contains("R&B/Soul")){
            agecriteria.setPopMusic(agecriteria.getRnbMusic()+1);
        }
        if(genre.contains("Religious")){
            agecriteria.setPopMusic(agecriteria.getReligiousMusic()+1);
        }
        if(genre.contains("Country")){
            agecriteria.setPopMusic(agecriteria.getCountryMusic()+1);
        }
        if(genre.contains("Rock")){
            agecriteria.setPopMusic(agecriteria.getRockMusic()+1);
        }
        if(genre.contains("Hip-Hop/Rap")){
            agecriteria.setPopMusic(agecriteria.getHiphopMusic()+1);
        }
        userService.saveAgeCriteria(agecriteria);
    }

    public String[] cosineMatrix(String currentuserId){
        ArrayList<String> allusers = new ArrayList<>();
//        ArrayList<User> users = userService.findOtherUser(currentuser);
        ArrayList<String> users = userService.findDistinctUser(currentuserId);
        ArrayList<String> vidhistID = videoService.findDistinctVid();
        ArrayList<String> videohist = new ArrayList<>();
        ArrayList<String> videorating = new ArrayList<>();
        String uhist;
        String currentU = userService.findCurrentByUserId(currentuserId);
        System.out.println(currentU);
        allusers.add(currentU);

        int count = 0;

        for (int i=0; i < users.size(); i++){
            allusers.add(users.get(i));
        }

        for (int i = 0; i < allusers.size(); i++) {
            System.out.println(allusers.get(i));
        }

        for(int j=0; j < vidhistID.size(); j++){
            VideoDetails vid = videoService.findByVideoid(vidhistID.get(j));
            videohist.add(vid.getVideoid());
            System.out.println("ASDSAD "+videohist.get(j));
        }
        String[] currentuserRow = new String[videohist.size()];
        for(int i=0; i < allusers.size(); i++){
            System.out.print(allusers.get(i));
            for(int j=0; j < vidhistID.size(); j++){
                VideoDetails vid = videoService.findByVideoid(vidhistID.get(j));
//                UserPreference genweight = userService.findgenreWeightByGenreNameandUserId();
                System.out.println("XXXXX "+allusers.get(i)+" "+vid.getVideoid()+" XXXXXX");
                uhist =  videoService.findByUserIdandVideoid(allusers.get(i),vid.getVideoid());
                System.out.println(uhist);
                if(uhist == null){
                    uhist = "0";
                }
                videorating.add(uhist);
            }
        }

        System.out.println(videohist.size());
        System.out.println(videorating.size());
        String[] otherRow = new String[videorating.size()];

        for (int i=0; i < videohist.size(); i++){
            System.out.printf("%15s", videohist.get(i));
        }
        System.out.println();
        for (int i=0; i < allusers.size(); i++){
            System.out.print(allusers.get(i));
            for (int j = 0; j < videohist.size(); j++) {
                System.out.printf("%15s", videorating.get(count));
                if(i == 0){
                    currentuserRow[j] = videorating.get(count);
                }
                else{
                    otherRow[count] = videorating.get(count);
                }
                count++;
            }
            System.out.println("");
        }

        String[] otheruserRow = new String[videohist.size()];

        count = 0;

        double[] cosineValue = new double[allusers.size()];
        double similarUser = 0;
        double temp;
        String[] arrUser = new String[allusers.size()];
        String simUser = "", tempotheruser;
        for (int i = 0; i < currentuserRow.length; i++) {
            System.out.print(currentuserRow[count]+" ");
            count++;
        }
        for (int i=1; i < allusers.size(); i++){
            for (int j = 0; j < videohist.size(); j++) {
                otheruserRow[j] = otherRow[count];
                System.out.print(otheruserRow[j]+" ");
                count++;
            }
            cosineValue[i] = cosineSimilarity(currentuserRow, otheruserRow, allusers.get(0), allusers.get(i), videohist);
            System.out.println("");
        }
        String[] similarUserRow = new String[videohist.size()];
        count = 0;

        for (int i = 0; i < currentuserRow.length; i++) {
            System.out.print(currentuserRow[count]+" ");
            count++;
        }
        for (int i = 0; i < 3; i++) {
            arrUser[i] = allusers.get(i);
        }

//        for (int i = 0; i < cosineValue.length; i++)
//        {
//            for (int j = i + 1; j < cosineValue.length; j++)
//            {
//                if (cosineValue[i] < cosineValue[j])
//                {
//                    temp = cosineValue[i];
//                    cosineValue[i] = cosineValue[j];
//                    cosineValue[j] = temp;
//                }
//            }
//        }

        for (int i = 0; i < arrUser.length; i++) {
            System.out.println(arrUser[i]);
        }
        System.out.println("OOOOOO - "+cosineValue.length);
        for (int i = 0; i < arrUser.length; i++) {
            for (int j = i + 1; j < cosineValue.length; j++)
            {
                if (cosineValue[i] < cosineValue[j])
                {
                    temp = cosineValue[i];
                    tempotheruser = arrUser[i];
                    cosineValue[i] = cosineValue[j];
                    arrUser[i] = arrUser[j];
                    cosineValue[j] = temp;
                    arrUser[j] = tempotheruser;
                }
            }
        }
        for (int i = 0; i < arrUser.length; i++) {
            System.out.println(arrUser[i]+": "+cosineValue[i]);
        }
//        System.out.println(simUser);
//        System.out.println(similarUser);
        String[] recommVids = ratingPrediction(currentuserRow, allusers.get(0), arrUser, cosineValue, videohist);
        return recommVids;
    }

    public double cosineSimilarity(String[] currentuserRatings, String[] otheruserRatings, String currentuser, String otheruser, ArrayList<String> allvideo){
        double nume = 0;
        double denum = 0;
        double cosineresult = 0;
        double multiplier1 = 0, multiplier2 = 0;
        System.out.println(allvideo.size()+" "+currentuserRatings.length+" "+otheruserRatings.length);
        System.out.println("=====================================================");
        for (int i=0; i < allvideo.size(); i++){
            System.out.printf("%15s", allvideo.get(i));
        }
        System.out.println();
        System.out.print(currentuser);
        for (int i = 0; i < currentuserRatings.length; i++) {
            System.out.printf("%13s", currentuserRatings[i]);
        }
        System.out.println();
        System.out.print(otheruser);
        for (int i = 0; i < otheruserRatings.length; i++) {
            System.out.printf("%13s", otheruserRatings[i]);
        }

        for (int i = 0; i < allvideo.size(); i++) {
            nume += Integer.parseInt(currentuserRatings[i])*Integer.parseInt(otheruserRatings[i]);
            multiplier1 += Double.parseDouble(currentuserRatings[i]) * Double.parseDouble(currentuserRatings[i]);
            multiplier2 += Double.parseDouble(otheruserRatings[i]) * Double.parseDouble(otheruserRatings[i]);
        }

        System.out.println("");
        System.out.println("NUMERATOR: " + nume);
        System.out.println("MULTIPLIER1: "+ multiplier1);
        System.out.println("MULTIPLIER2: "+ multiplier2);
        System.out.println("SQUARE1: "+ Math.sqrt(multiplier1));
        System.out.println("SQUARE2: "+ Math.sqrt(multiplier2));
        denum = Math.sqrt(multiplier1) * Math.sqrt(multiplier2);
        System.out.println("DENUMERATOR: "+ denum);
        cosineresult = nume/denum;
        System.out.println("RESULT: "+ cosineresult);
        System.out.println();

//        System.out.println((int)Math.sqrt(25));

        return cosineresult;
    }

    public String[] ratingPrediction(String[] currentuserRatings, String currentuser, String[] otheruser, double[] cosineValue, ArrayList<String> allvideo){
        String uhist;
        double nume = 0;
        double denum = 0;
        double[] predictedRate = new double[allvideo.size()];
        String[] predictedVidId = new String[allvideo.size()];
        String[][] otherUserRating = new String[otheruser.length-1][allvideo.size()];
        ArrayList<String> videorating = new ArrayList<>();
        for(int i=0; i < otheruser.length-1; i++){
            System.out.print(otheruser[i]);
            for(int j=0; j < allvideo.size(); j++){
                VideoDetails vid = videoService.findByVideoid(allvideo.get(j));
                System.out.println("XXXXX "+otheruser[i]+" "+vid.getVideoid()+" XXXXXX");
                uhist =  videoService.findByUserIdandVideoid(otheruser[i],vid.getVideoid());
                System.out.println(uhist);
                if(uhist == null){
                    uhist = "0";
                }
                otherUserRating[i][j] = uhist;
                videorating.add(uhist);
            }
        }

        for (int i = 0; i < otheruser.length-1; i++) {
            System.out.println("SIM("+currentuser+","+otheruser[i]+"): "+ cosineValue[i]);
        }
        System.out.println("RATING PREDICTION: ");
        for (int i=0; i < allvideo.size(); i++){
            System.out.printf("%15s", allvideo.get(i));
        }
        System.out.println();
        System.out.print(currentuser);
        for (int i = 0; i < currentuserRatings.length; i++) {
            System.out.printf("%13s", currentuserRatings[i]);
        }
        System.out.println();
        for (int i = 0; i < otheruser.length-1; i++) {
            System.out.print(otheruser[i]);
            for (int j = 0; j < allvideo.size(); j++) {
                System.out.printf("%13s", otherUserRating[i][j]);
            }
            System.out.println(" ");
        }

        for (int i = 0; i < allvideo.size(); i++) {
            for (int j = 0; j < otheruser.length-1; j++) {
                if(Double.parseDouble(currentuserRatings[i]) == 0){
                    nume += cosineValue[j] * Double.parseDouble(otherUserRating[j][i]);
                    denum += cosineValue[j];
                    predictedRate[i] = nume/denum;
                    predictedVidId[i] = allvideo.get(i);
                }
            }
            nume = 0;
            denum = 0;
        }
        double temp = 0;
        String tempvidId = "";
        for (int i = 0; i < predictedVidId.length; i++) {
//            System.out.println(predictedVidId[i]+": "+predictedRate[i]);
            for (int j = i + 1; j < predictedVidId.length; j++)
            {
                if (predictedRate[i] < predictedRate[j])
                {
                    temp = predictedRate[i];
                    tempvidId = predictedVidId[i];
                    predictedRate[i] = predictedRate[j];
                    predictedVidId[i] = predictedVidId[j];
                    predictedRate[j] = temp;
                    predictedVidId[j] = tempvidId;
                }
            }
        }
        for (int i = 0; i < predictedVidId.length; i++) {
            System.out.println(predictedVidId[i]+": "+predictedRate[i]);
        }
        DecimalFormat numberFormat = new DecimalFormat("#");
//        System.out.println(numberFormat.format(predictedRate[1])+"KASGDKSJGADJKGASD");
//        updateRating(predictedVidId, predictedRate, currentuser);
        return predictedVidId;
    }

    public void updateRating(String[] predictedVidId, double[] predictedRate, String currentuser){
        DecimalFormat numberFormat = new DecimalFormat("#");
        for (int i = 0; i < predictedVidId.length; i++) {
            UserHistory currentuserhist = new UserHistory();
            if(predictedVidId[i] != null) {
                currentuserhist.setUserId(currentuser);
                currentuserhist.setVideoid(predictedVidId[i]);
                currentuserhist.setVidRating(numberFormat.format(predictedRate[i]));
                userService.saveUserHistory(currentuserhist);
                currentuserhist = null;

            }
        }
    }

    @RequestMapping("/vplayer")
    public String showVideoPlayer() {
        return "VideoPlayerV2";
    }


    @RequestMapping("/profile")
    public String showUserProfile(HttpServletRequest request, Model model) {
        List<Video> videoList = videoService.findAll();
        for (int i = 0; i < videoList.size(); i++) {
            System.out.println(videoList.get(i).getVideoid());
        }
        model.addAttribute("vids", videoList);
        return "UserProfile";
    }



    public void saveMV(String vidId, String title, String url) {
        Video newVideo = new Video();
        newVideo.setVideoid(vidId);
        newVideo.setMvtitle(title);
        newVideo.setThumbnail(url);
        videoService.saveVideo(newVideo);
    }




    @RequestMapping("/homefeed")
    public String showHomeFeed(Model model){
        return "HomeFeed";
    }

    @RequestMapping("/explore")
    public String showExplore(Model model){
        ArrayList<Genre> genres = videoService.findAllGenre();

        model.addAttribute("genre", genres);

        return "Explore";
    }

    @RequestMapping("/sidemenu")
    public String sideMenu(HttpServletRequest request, Model model){

        String explore = request.getParameter("explore");
        System.out.println(explore);
        System.out.println("bobo");
        ArrayList<Genre> genres = videoService.findAllGenre();

        model.addAttribute("genre", genres);
        return showExplore(model);

    }

    @RequestMapping("/admin")
    public String Admin(HttpServletRequest request, Model model){
        return "AdminPage";
    }

    public String concat(String x){
        NumberFormat val = NumberFormat.getNumberInstance(Locale.US);;
        String out = val.format(Long.valueOf(x));
        return out;
    }
}