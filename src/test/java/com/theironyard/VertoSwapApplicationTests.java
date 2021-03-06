package com.theironyard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theironyard.entities.Item;
import com.theironyard.entities.User;
import com.theironyard.entities.Work;
import com.theironyard.services.*;
import com.theironyard.utilities.PasswordStorage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VertoSwapApplication.class)
@WebAppConfiguration

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class VertoSwapApplicationTests {

	@Autowired
	WebApplicationContext wac;

	@Autowired
	UserRepository users;
	@Autowired
	ItemRepository items;
	@Autowired
	WorkRepository works;
	@Autowired
	PhotoRepository photos;
	@Autowired
	MessageaRepository messages;

	MockMvc mockMvc;

	@Before
	public void before() {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	@Test
	public void aTestCreateAccount() throws Exception {
		mockMvc.perform(
				MockMvcRequestBuilders.post("/account-create")
				.param("username", "Bob")
				.param("password", "123")
		);
		Assert.assertTrue(PasswordStorage.verifyPassword("123", users.findByUsername("Bob").getPassword()));
	}

	@Test
	public void bTestLogin() throws Exception {
		mockMvc.perform(
				MockMvcRequestBuilders.post("/login")
				.param("username", "Bob")
				.param("password", "123")
		);
		Assert.assertTrue(PasswordStorage.verifyPassword("123", users.findByUsername("Bob").getPassword()));
	}

	@Test
	public void cTestItemCreate() throws Exception {
		mockMvc.perform(
				MockMvcRequestBuilders.post("/item-create")
				.sessionAttr("username", "Bob")
				.param("title", "TestTitle")
				.param("location", "TestLocation")
				.param("description", "TestDescription")
				.param("acceptableExchange", "TestSwap")
				.param("service", "true")
		);

		Iterable<Item> testItems = items.findByUser(users.findByUsername("Bob"));
		ArrayList<Item> testArr = new ArrayList<>();
		for (Item i : testItems) {
			testArr.add(i);
		}
		Assert.assertTrue(testArr.size() == 1);
	}

	@Test
	public void dTestWorkCreate() throws Exception {
		mockMvc.perform(
				MockMvcRequestBuilders.post("/work-create")
				.sessionAttr("username", "Bob")
				.param("jobTitle", "TestJob")
				.param("description", "TestJobDesc")
		);
		ArrayList<Work> workArr = (ArrayList<Work>) works.findByUser(users.findByUsername("Bob"));
		Assert.assertTrue(workArr.size() == 1);

	}

	@Test
	public void eTestAttachWork() throws Exception {
		ArrayList<Work> workArr = (ArrayList<Work>) works.findByUser(users.findByUsername("Bob"));
		Work w = workArr.get(0);
		ArrayList<Item> itemArr = (ArrayList<Item>) items.findByUser(users.findByUsername("Bob"));
		Item i = itemArr.get(0);

		mockMvc.perform(
				MockMvcRequestBuilders.post("/item-attach-work")
						.sessionAttr("username", "Bob")
						.param("id", String.valueOf(i.getId()))
						.param("workId", String.valueOf(w.getId()))
		);
		itemArr = (ArrayList<Item>) items.findByUser(users.findByUsername("Bob"));
		Assert.assertTrue(itemArr.get(0).getWork().getJobTitle().equals("TestJob"));
	}

	@Test
	public void fTestWorkDelete() throws Exception {
		ArrayList<Work> workArr = (ArrayList<Work>) works.findByUser(users.findByUsername("Bob"));
		Work w = workArr.get(0);

		mockMvc.perform(
				MockMvcRequestBuilders.post("/work-delete")
						.sessionAttr("username", "Bob")
						.param("id", String.valueOf(w.getId()))
		);

		workArr = (ArrayList<Work>) works.findByUser(users.findByUsername("Bob"));
		Assert.assertTrue(workArr.size() == 0);
	}

	@Test
	public void gTestItemArchive() throws Exception {
		ArrayList<Item> itemArr = (ArrayList<Item>) items.findByUser(users.findByUsername("Bob"));
		Item i = itemArr.get(0);
		mockMvc.perform(
				MockMvcRequestBuilders.post("/item-archive")
				.sessionAttr("username", "Bob")
				.param("id", String.valueOf(i.getId()))
		);
		itemArr = (ArrayList<Item>) items.findByUser(users.findByUsername("Bob"));
		Item item = itemArr.get(0);
		Assert.assertTrue(item.getStatus() == Item.Status.ARCHIVE);
	}

	@Test
	public void hTestItemDelete() throws Exception {
		ArrayList<Item> itemArr = (ArrayList<Item>) items.findByUser(users.findByUsername("Bob"));
		Item i = itemArr.get(0);
		mockMvc.perform(
				MockMvcRequestBuilders.post("/item-delete")
				.sessionAttr("username", "Bob")
				.param("id", String.valueOf(i.getId()))
		);
		itemArr = (ArrayList<Item>) items.findByUser(users.findByUsername("Bob"));
		Item item = itemArr.get(0);
		Assert.assertTrue(item.getStatus() == Item.Status.DELETE);
	}
}
