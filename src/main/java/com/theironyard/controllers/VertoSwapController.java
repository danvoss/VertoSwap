package com.theironyard.controllers;

import com.theironyard.entities.*;
import com.theironyard.entities.Thread;
import com.theironyard.services.*;
import com.theironyard.utilities.PasswordStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static com.theironyard.entities.Item.Status.ACTIVE;
import static com.theironyard.entities.Item.Status.ARCHIVE;
import static com.theironyard.entities.Item.Status.INACTIVE;


/**
 * Created by Dan on 7/19/16.
 */
@Controller
public class VertoSwapController
{

    public static String PHOTOS_DIR = "photos/";

    @Autowired
    UserRepository users;

    @Autowired
    WorkRepository works;

    @Autowired
    ItemRepository items;

    @Autowired
    MessageRepository messages;

    @Autowired
    ThreadRepository threads;

    @Autowired
    PhotoRepository photos;


    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String main(HttpSession session, Model model)
    {
        String username = (String) session.getAttribute("username");

        // add full text search //

        // filter by status
        Iterable<Item> servicesList = items.findByServiceTrueOrderByTimeDesc();
        Iterable<Item> goodsList = items.findByServiceFalseOrderByTimeDesc();

        model.addAttribute("username", username);
        model.addAttribute("services", servicesList);
        model.addAttribute("goods", goodsList);

        return "home";
    }

    @RequestMapping(path = "/account-create", method = RequestMethod.GET)
    public String createAccountPage(HttpSession session, Model model)
    {
        return "account-create";
    }

    @RequestMapping(path = "/user-profile", method = RequestMethod.GET )
    public String userProfile(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "home";
        }
        User user = users.findByUsername(username);
        //Iterable<Item> activeItems = items.findByUser(user);
        Iterable<Item > activeItems = items.findByUserAndStatusOrderByTimeDesc(user, ACTIVE);
        //Iterable<Item> activeItems = items.findByUserAndStatus(user, ACTIVE);
        Iterable<Item > inactiveItems = items.findByUserAndStatusOrderByTimeDesc(user, INACTIVE);
        //Iterable<Item> inactiveItems = items.findByUserAndStatus(user, INACTIVE);
        model.addAttribute("username", username);
        model.addAttribute("activeBarters", activeItems);
        return "user-profile";
    }

    @RequestMapping(path = "/archive", method = RequestMethod.GET)
    public String archive(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "home";
        }
        User user = users.findByUsername(username);
        Iterable<Item> archivedItems = items.findByUserAndStatus(user, ARCHIVE);
        model.addAttribute("username", username);
        model.addAttribute("archived", archivedItems);
        return "archive";
    }





    //***************************************************************************************
    //
    //USER ROUTES
    //
    //***************************************************************************************
    @RequestMapping(path = "/account-create", method = RequestMethod.POST)
    public String createAccount(HttpSession session, String username, String password) throws Exception {
        User user = users.findByUsername(username);
        if (user != null) {
            throw new Exception("Username unavailable.");
        }
        else {
            user = new User(username, PasswordStorage.createHash(password));
            users.save(user);
            session.setAttribute("username", username);
        }
        return "redirect:/";
    }

    @RequestMapping(path = "/account-update", method = RequestMethod.POST)
    public String editAccount(HttpSession session, String password, String newUsername, String newPassword) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in.");
        }
        User user = users.findByUsername(username);
        if (!PasswordStorage.verifyPassword(password, user.getPassword())) {
            throw new Exception("Wrong password.");
        }
        user.setUsername(newUsername);
        user.setPassword(PasswordStorage.createHash(newPassword));
        users.save(user);
        return "redirect:/";
    }

    @RequestMapping(path = "/account-delete", method = RequestMethod.POST)
    public String deleteAccount(HttpSession session) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in.");
        }
        User user = users.findByUsername(username);
        items.delete(items.findByUser(user));
        works.delete(works.findByUser(user));
        photos.delete(photos.findByUser(user));
        messages.delete(messages.findByUser(user));
        users.delete(user.getId());
        return "redirect:/";
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(HttpSession session, String username, String password) throws Exception {
        User userFromDB = users.findByUsername(username);
        if (userFromDB == null) {
            return "redirect:/create-account";
        }
        else if (!PasswordStorage.verifyPassword(password, userFromDB.getPassword())) {
            throw new Exception("Wrong password.");
        }
        session.setAttribute("username", username);
        return "redirect:/";
    }

    @RequestMapping(path = "/logout", method = RequestMethod.POST)
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }


    //***************************************************************************************
    //
    //WORK ROUTES
    //
    //***************************************************************************************
    @RequestMapping(path = "/work-create", method = RequestMethod.POST)
    public String createWork(HttpSession session,String job_title, String description)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        Work w = new Work(job_title, description, user);
        works.save(w);
        session.setAttribute("username", user.getUsername());
        return "redirect:/user-profile";
    }

    @RequestMapping(path = "/work-read", method = RequestMethod.GET)
    public String getWork(HttpSession session)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        Iterable<Work> workList = works.findByUser(user);
        session.setAttribute("username", user.getUsername());
        return"";
    }

    @RequestMapping(path = "/work-update", method = RequestMethod.POST)
    public String updateWork(HttpSession session, int id, String job_title, String description)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        Work w = new Work(job_title, description, user);
        w.setId(id);
        works.save(w);
        session.setAttribute("username", user.getUsername());
        return "redirect:/";
    }

    @RequestMapping(path = "/work-delete", method = RequestMethod.POST)
    public String deleteWork(HttpSession session, int id)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        works.delete(id);
        session.setAttribute("username", user.getUsername());
        return "redirect:/";
    }

    //***************************************************************************************
    //
    //ITEM ROUTES
    //
    //***************************************************************************************
    @RequestMapping(path = "/item-create", method = RequestMethod.POST)
    public String createItem(HttpSession session, String title, String location, String description, String acceptableExchange, boolean service)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        LocalDateTime time = LocalDateTime.now();
        //Item.Status status = ACTIVE;
        Item i = new Item(title, location, description, acceptableExchange, ACTIVE, time, service, user);
        items.save(i);
        session.setAttribute("username", user.getUsername());
        return "redirect:/user-profile";
    }

    @RequestMapping(path = "/item-read-specific", method = RequestMethod.GET)
    public String getSpecificItem(HttpSession session, int id)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        Item item = items.findOne(id);
        session.setAttribute("username", user.getUsername());
        return"";
    }

    @RequestMapping(path = "/item-read", method = RequestMethod.GET)
    public Iterable<Item> getItem(HttpSession session)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        Iterable<Item> itemsList = items.findByUser(user);
        for(Item item : itemsList)
        {
            if(item.getTime().plusWeeks(2).compareTo(LocalDateTime.now()) > 0)
            {
                item.setStatus(INACTIVE);
                items.save(item);
            }
        }
        session.setAttribute("username", user.getUsername());
        return itemsList;
    }

    @RequestMapping(path = "/item-update", method = RequestMethod.POST)
    public String updateItem(HttpSession session, int id, String title, String location, String description, String acceptableExchange, String stat, boolean service)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        LocalDateTime time = LocalDateTime.now();
        Item.Status status = Item.Status.valueOf(stat);
        Item i = new Item(title, location, description, acceptableExchange, status, time, service, user);
        i.setId(id);
        items.save(i);
        session.setAttribute("username", user.getUsername());
        return "redirect:/";
    }

    @RequestMapping(path = "/item-delete", method = RequestMethod.POST)
    public String deleteitem (HttpSession session, int id)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        items.delete(id);
        session.setAttribute("username", user.getUsername());
        return "redirect:/";
    }


    //***************************************************************************************
    //
    //                  THREAD ROUTES
    //
    //***************************************************************************************
    @RequestMapping(path = "/thread-create", method = RequestMethod.POST)
    public String createThread(HttpSession session, User receiver, Item item)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        Thread t = new Thread(user,receiver, item);
        threads.save(t);
        session.setAttribute("username", user.getUsername());
        return "redirect:/";
    }

    @RequestMapping(path = "/thread-read", method = RequestMethod.GET)
    public Iterable<Thread> getThread(HttpSession session)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        Iterable<Thread> threadList = threads.findByReceiver(user);
        session.setAttribute("username", user.getUsername());
        return threadList;
    }

    @RequestMapping(path = "/thread-update", method = RequestMethod.POST)
    public String updateThread(HttpSession session,int id, User receiver, Item item)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        Thread t = new Thread(user,receiver, item);
        t.setId(id);
        threads.save(t);
        session.setAttribute("username", user.getUsername());
        return "redirect:/";
    }

    @RequestMapping(path = "/thread-delete", method = RequestMethod.POST)
    public String deleteThread(HttpSession session, int id)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        threads.delete(id);
        session.setAttribute("username", user.getUsername());
        return "redirect:/";

    }





    //***************************************************************************************
    //
    //           PHOTO ROUTES
    //
    //***************************************************************************************
    @RequestMapping(path = "/photo-create", method = RequestMethod.POST)
    public String addPhoto(HttpSession session, MultipartFile photo, String filename, String caption, /*Item item,*/ int id, HttpServletResponse response) throws Exception
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        File dir = new File("public/" + PHOTOS_DIR);
        dir.mkdirs();

        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), dir);
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        Item item = items.findOne(id);

        Photo newPhoto = new Photo(photoFile.getName(), caption, user, item);
        photos.save(newPhoto);
        session.setAttribute("username", user.getUsername());
        return "redirect:/user-profile";
    }

    @RequestMapping(path = "/photo-read", method = RequestMethod.GET)
    public String getPhoto(HttpSession session, Item item)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        Iterable<Photo> photoList = photos.findByItem(item);
        session.setAttribute("username", user.getUsername());
        return"";
    }

    @RequestMapping(path = "/photo-update", method = RequestMethod.POST)
    public String updatePhoto(HttpSession session, MultipartFile photo, int id, String filename, String caption, Item item, HttpServletResponse response) throws Exception
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        File dir = new File("public/" + PHOTOS_DIR);
        dir.mkdirs();

        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), dir);
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        Photo newPhoto = new Photo(photoFile.getName(), caption, user, item);
        newPhoto.setId(id);
        photos.save(newPhoto);
        session.setAttribute("username", user.getUsername());
        return "redirect:/";
    }

    @RequestMapping(path = "/photo-delete", method = RequestMethod.POST)
    public String deletePhoto(HttpSession session, int id)
    {
        String username = (String)session.getAttribute("username");
        User user = users.findByUsername(username);
        works.delete(id);
        session.setAttribute("username", user.getUsername());
        return "redirect:/";

    }

}
