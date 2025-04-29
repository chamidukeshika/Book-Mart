package com.ecom.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Cart;
import com.ecom.model.Category;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private CartService cartService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;

	@GetMapping("/")
	public String home() {
		return "user/home";
	}

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {

		if (p != null) {

			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);

		}
		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);

	}

	@GetMapping("/addCart")
	public String addToCart(@RequestParam Integer pid, @RequestParam Integer uid, HttpSession session) {

		Cart saveCart = cartService.saveCart(pid, uid);
		if (ObjectUtils.isEmpty(saveCart)) {
			session.setAttribute("errorMsg", "Product Adding Failed");
		} else {
			session.setAttribute("succMsg", "Product added to cart!!");

		}

		return "redirect:/product/" + pid;
	}

	@GetMapping("/cart")
	public String loadCartPage(Principal p, Model m) {

		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);

		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartByUser(user.getId());

		m.addAttribute("carts", carts);

		if (carts.size() > 0) {
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();

			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/cart";
	}

	@GetMapping("/cartQuantityUpdate")

	public String updteCartQuantity(@RequestParam String sy, @RequestParam Integer cid) {

		cartService.updateQuantity(sy, cid);
		return "redirect:/user/cart";
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	@GetMapping("/orders")
	public String orderPage(Principal p, Model m) {

		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartByUser(user.getId());

		m.addAttribute("carts", carts);

		if (carts.size() > 0) {
			Double orderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice() + 250 + 100;

			m.addAttribute("totalOrderPrice", totalOrderPrice);
			m.addAttribute("orderPrice", orderPrice);
		}

		return "/user/order";
	}

	@PostMapping("/orders/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p, HttpSession session) {
		try {
			UserDtls user = getLoggedInUserDetails(p);
			orderService.saveOrder(user.getId(), request);
			return "redirect:/user/success";
		} catch (Exception e) {
			session.setAttribute("errorMsg",
					"Insufficient stocks to fulfill the order. Please review and adjust the order quantities");
			return "redirect:/user/cart";
		}
	}

	@GetMapping("/success")
	public String loadSucces() {

		return "/user/success";
	}

	@GetMapping("/user-orders")
	public String myOrder(Model m, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p); // Method to get logged-in user details
		List<ProductOrder> orders = orderService.getOrdersByUser(loginUser.getId()); // Fetch orders for the user

		// Sort orders by orderDate field in descending order
		orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate())); // Sorting in descending order

		m.addAttribute("orders", orders); // Pass orders to the Thymeleaf template
		return "/user/my_orders"; // Return the view path
	}

	@GetMapping("/update-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session)
			throws Exception {

		OrderStatus[] values = OrderStatus.values();

		String status = null;

		for (OrderStatus orderSt : values) {

			if (orderSt.getId().equals(st)) {
				status = orderSt.getName();
			}

		}

		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);
		commonUtil.sendMailForProductOrder(updateOrder, status);
		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Order Cancelled Successfully !");

		} else {
			session.setAttribute("errorMsg", "status not updated");

		}

		return "redirect:/user/user-orders";
	}

	@GetMapping("/profile")
	public String profile() {
		return "/user/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {

		UserDtls updateUserProfile = userService.updateUserProfile(user, img);

		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("succMsg", "Profile Updated Successfully !");

		} else {
			session.setAttribute("errorMsg", "Something went wrong on update");

		}
		return "redirect:/user/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {

		UserDtls loggedInUserDetails = getLoggedInUserDetails(p); // Method to get logged-in user details

		boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());

		if (matches) {

			String encodePassword = passwordEncoder.encode(newPassword);
			loggedInUserDetails.setPassword(encodePassword);
			UserDtls updateUser = userService.updateUser(loggedInUserDetails);

			if (ObjectUtils.isEmpty(updateUser)) {
				session.setAttribute("errorMsg", "Password not updated || Error in Server");
			} else {
				session.setAttribute("succMsg", "Password Updated Successfully !");
			}

		} else {
			session.setAttribute("errorMsg", "Current Password is incorrect");

		}
		return "redirect:/user/profile";
	}

//	// sms
//
//	@Value("${twilio.account_sid}")
//	String accountSID;
//
//	@Value("${twilio.auth.token}")
//	String authToken;
//
//	@Value("${twilio.mobile_number}")
//	String mobileNumber;
//
//	public void sentSMS(String toMobile, String message) {
//
//		Twilio.init(accountSID, authToken);
//
//		Message.creator(new PhoneNumber(toMobile), new PhoneNumber(mobileNumber), message).create();
//	}

}
