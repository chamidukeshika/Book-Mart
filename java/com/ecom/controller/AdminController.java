package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.ReadOnline;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.ReadOnlineService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private PasswordEncoder passwordEncoder;

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

	@GetMapping("/")
	public String index() {
		return "admin/index";
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model m) {
		List<Category> categories = categoryService.getAllCategory();
		m.addAttribute("categories", categories);
		return "admin/add_product";
	}

	@GetMapping("/category")
	public String category(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

		// m.addAttribute("categorys", categoryService.getAllCategory());

		Page<Category> page = categoryService.getAllCategoryPagination(pageNo, pageSize);

		List<Category> categorys = page.getContent();
		m.addAttribute("categorys", categorys);
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/category";
	}

	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		String imageName = file != null ? file.getOriginalFilename() : "default.jpg";
		category.setImageName(imageName);

		Boolean existCategory = categoryService.existCategory(category.getName());

		if (existCategory) {

			session.setAttribute("errorMsg", "Category Name Already Exists");
		} else {
			Category saveCategory = categoryService.saveCategory(category);

			if (ObjectUtils.isEmpty(saveCategory)) {
				session.setAttribute("errorMsg", "Not Saved ! Internal server error");
			} else {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());
				System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				session.setAttribute("succMsg", "Saved successfully");
			}
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id, HttpSession session) {
		Boolean deleteCategory = categoryService.deleteCategory(id);
		if (deleteCategory) {
			session.setAttribute("succMsg", "Category Deleted Successfully");
		} else {
			session.setAttribute("errorMsg", "Something Went Wrong");
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id, Model m) {
		m.addAttribute("category", categoryService.getCategoryById(id));
		return "admin/edit_category";
	}

	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {
		Category Oldcategory = categoryService.getCategoryById(category.getId());
		String imageName = file.isEmpty() ? Oldcategory.getImageName() : file.getOriginalFilename();

		if (!ObjectUtils.isEmpty(category)) {

			Oldcategory.setName(category.getName());
			Oldcategory.setIsActive(category.getIsActive());
			Oldcategory.setImageName(imageName);
		}

		Category updateCategory = categoryService.saveCategory(Oldcategory);

		if (!ObjectUtils.isEmpty(updateCategory)) {

			if (!file.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());
				System.out.println(path);

//				print path
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			session.setAttribute("succMsg", "Category Updated Successfully");

		} else {
			session.setAttribute("succMsg", "Something Went Wrong");

		}

		return "redirect:/admin/loadEditCategory/" + category.getId();
	}

	@PostMapping("/saveProduct")
	public String saveProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session) throws IOException {

		String imageName = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();

		product.setImage(imageName);
		product.setDiscount(0);
		product.setDiscountPrice(product.getPrice());
		Product saveProduct = productService.saveProduct(product);

		if (!ObjectUtils.isEmpty(saveProduct)) {
			File saveFile = new ClassPathResource("static/img").getFile();

			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "product_img" + File.separator
					+ image.getOriginalFilename());
			// System.out.println(path);

			Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			session.setAttribute("succMsg", "Product Saved Successfully");
		} else {
			session.setAttribute("errorMsg", "Something went wrong on server");
		}

		return "redirect:/admin/loadAddProduct";
	}

	@GetMapping("/products")
	public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch, HttpSession session,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {

		Page<Product> page = null;
		if (ch != null && ch.length() > 0) {
			page = productService.searchProductPagination(pageNo, pageSize, ch);
			if (page == null || page.isEmpty()) {
				session.setAttribute("errorMsg", "Incorrect search !! No products found.");
			} else {
				session.removeAttribute("errorMsg");
			}
		} else {
			page = productService.getAllProductsPagination(pageNo, pageSize);
			session.removeAttribute("errorMsg");
		}
		m.addAttribute("products", page.getContent());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/products";
	}

	@GetMapping("/deleteProduct/{id}")
	public String deleteProduct(@PathVariable int id, HttpSession session) {

		Boolean deleteProduct = productService.deleteProduct(id);
		if (deleteProduct) {
			session.setAttribute("succMsg", "Product Deleted Successfully");
		} else {
			session.setAttribute("errorMsg", "Something Went Wrong");
		}

		return "redirect:/admin/products";
	}

	@GetMapping("/editProduct/{id}")
	public String loadEditProduct(@PathVariable int id, Model m) {
		m.addAttribute("product", productService.getProductById(id));
		m.addAttribute("categories", categoryService.getAllCategory());
		return "admin/edit_product";
	}

	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session) throws IOException {

		if (product.getDiscount() < 0 || product.getDiscount() > 100) {
			session.setAttribute("errorMsg", "Invalid Discount");

		} else {
			Product updateProduct = productService.updateProduct(product, image);

			if (!ObjectUtils.isEmpty(updateProduct)) {
				session.setAttribute("succMsg", "Product Updated Successfully");
			} else {
				session.setAttribute("errorMsg", "Something Went Wrong");
			}
		}

		return "redirect:/admin/editProduct/" + product.getId();
	}

	@GetMapping("/users")
	public String getAllUsers(Model m, @RequestParam Integer type) {

		List<UserDtls> users = null;

		if (type == 1) {
			users = userService.getUsers("ROLE_USER");

		} else {
			users = userService.getUsers("ROLE_ADMIN");
		}
		m.addAttribute("userType", type);
		m.addAttribute("users", users);
		return "/admin/users";
	}

	@GetMapping("/updateSts")
	public String updateUserAccountStatus(@RequestParam Boolean status, @RequestParam Integer id,
			@RequestParam Integer type, HttpSession session) {

		Boolean f = userService.updateAccountStatus(id, status);

		if (f) {
			session.setAttribute("succMsg", "Account Status Updated !");
		} else {
			session.setAttribute("errorMsg", "Something went wrong !");

		}
		return "redirect:/admin/users?type=" + type;

	}

	@GetMapping("/orders")
	public String getAllOrders(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {
//		List<ProductOrder> orders = orderService.getAllOrders();
//
//		orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
//
//		m.addAttribute("orders", orders);
//		m.addAttribute("srch", false);

		Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "orderDate"));

		Page<ProductOrder> page = orderService.getAllOrdersPagination(pageable);

		// Manually sort the content of the page using streams
		List<ProductOrder> sortedOrders = page.getContent().stream()
				.sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate())) // Sort by orderDate descending
				.collect(Collectors.toList());

		m.addAttribute("orders", sortedOrders);
		m.addAttribute("srch", false);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "/admin/orders";
	}

	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {

		OrderStatus[] values = OrderStatus.values();

		String status = null;

		for (OrderStatus orderSt : values) {

			if (orderSt.getId().equals(st)) {
				status = orderSt.getName();
			}

		}

		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);

		try {
			commonUtil.sendMailForProductOrder(updateOrder, status);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Order Updated Successfully !");

		} else {
			session.setAttribute("errorMsg", "status not updated");

		}

		return "redirect:/admin/orders";
	}

	// search product
	@GetMapping("/search-order")
	public String searchProduct(@RequestParam String orderId, Model m, HttpSession session,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {

		if (orderId != null && orderId.length() > 0) {
			ProductOrder order = orderService.getOrderByOrderId(orderId.trim());

			if (ObjectUtils.isEmpty(order)) {

				session.setAttribute("errorMsg", "Incorrect Order Id !!!");
				m.addAttribute("orderDtls", null);
			} else {
				m.addAttribute("orderDtls", order);
			}

			m.addAttribute("srch", true);
		} else {
			Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
			m.addAttribute("orders", page);
			m.addAttribute("srch", false);

			m.addAttribute("pageNo", page.getNumber());
			m.addAttribute("pageSize", pageSize);
			m.addAttribute("totalElements", page.getTotalElements());
			m.addAttribute("totalPages", page.getTotalPages());
			m.addAttribute("isFirst", page.isFirst());
			m.addAttribute("isLast", page.isLast());

		}
		return "/admin/orders";
	}

	// search product
	@GetMapping("/search")
	public String searchProduct(@RequestParam String ch, Model m) {
		List<Product> searchProduct = productService.searchProduct(ch);
		m.addAttribute("products", searchProduct);

		return "admin/products";
	}

	@GetMapping("/add-admin")
	public String loadAdminAdd() {

		return "/admin/add_admin";
	}

	@PostMapping("/save-admin")
	public String saveAdmin(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
			throws IOException {

		Boolean existsEmail = userService.existsEmail(user.getEmail());

		if (existsEmail) {
			session.setAttribute("errorMsg", "Admin Email already exist");
		} else {

			String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
			user.setProfileImage(imageName);

			UserDtls saveUser = userService.saveAdmin(user);

			if (!ObjectUtils.isEmpty(saveUser)) {
				if (!file.isEmpty()) {

					File saveFile = new ClassPathResource("static/img").getFile();

					Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
							+ file.getOriginalFilename());
					// System.out.println(path);

					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				}
				session.setAttribute("succMsg", "Registration Successfull !");

			} else {
				session.setAttribute("succMsg", "Something Went Wrong");
			}
		}

		return "redirect:/admin/add-admin";
	}

	@GetMapping("/profile")
	public String profile() {
		return "/admin/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {

		UserDtls updateUserProfile = userService.updateUserProfile(user, img);

		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("succMsg", "Profile Updated Successfully !");

		} else {
			session.setAttribute("errorMsg", "Something went wrong on update");

		}
		return "redirect:/admin/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {

		UserDtls loggedInUserDetails = commonUtil.getLoggedInUserDetails(p); // Method to get logged-in user details

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
		return "redirect:/admin/profile";
	}

	@GetMapping("/product-analytics")
	public String productAnalytics(Model model) {
		List<Product> products = productService.getAllProducts();
		List<ProductOrder> orders = orderService.getAllOrders();

		List<Map<String, Object>> productAnalytics = products.stream().map(product -> {
			// Calculate total sold units (quantity) for each product
			long soldCount = orders.stream().filter(order -> order.getProduct().getId() == product.getId())
					.mapToLong(ProductOrder::getQuantity).sum();

			// Calculate total sales for each product
			double totalSales = orders.stream().filter(order -> order.getProduct().getId() == product.getId())
					.mapToDouble(order -> order.getPrice() * order.getQuantity()).sum();

			// Calculate Average Order Value (AOV)
			double totalOrderValue = orders.stream().filter(order -> order.getProduct().getId() == product.getId())
					.mapToDouble(order -> order.getPrice() * order.getQuantity()).sum();
			long totalOrders = orders.stream().filter(order -> order.getProduct().getId() == product.getId()).count();
			double averageOrderValue = totalOrders > 0 ? totalOrderValue / totalOrders : 0;

			// Calculate the most expensive sale (highest order value for this product)
			double mostExpensiveSale = orders.stream().filter(order -> order.getProduct().getId() == product.getId())
					.mapToDouble(order -> order.getPrice() * order.getQuantity()).max().orElse(0);

			// Calculate average quantity per order
			double averageQuantityPerOrder = totalOrders > 0 ? (double) soldCount / totalOrders : 0;

			// Prepare the analytics data in a map
			Map<String, Object> analytics = new HashMap<>();
			analytics.put("productName", product.getTitle());
			analytics.put("soldCount", soldCount);
			analytics.put("totalSales", totalSales);
			analytics.put("averageOrderValue", averageOrderValue);
			analytics.put("mostExpensiveSale", mostExpensiveSale);
			analytics.put("averageQuantityPerOrder", averageQuantityPerOrder);
			analytics.put("totalOrders", totalOrders);

			return analytics;
		}).collect(Collectors.toList());

		// Add the product analytics data to the model
		model.addAttribute("productAnalytics", productAnalytics);
		return "/admin/product_analytics"; // Your view name
	}

	// read online

	@Autowired
	private ReadOnlineService readOnlineService;

	@GetMapping("/library")
	public String manageLibrary(Model model, @RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "5") int pageSize) {
		Page<ReadOnline> page = readOnlineService.getAllDocuments("", pageNo, pageSize);
		model.addAttribute("documents", page.getContent());
		model.addAttribute("currentPage", pageNo);
		model.addAttribute("totalPages", page.getTotalPages());
		return "admin/library";
	}

	@GetMapping("/add-document")
	public String showAddDocument(Model model) {
		model.addAttribute("document", new ReadOnline());
		// Add categories to the model
		model.addAttribute("categories", categoryService.getAllActiveCategory());
		return "admin/add_document";
	}

	@PostMapping("/save-document")
	public String saveDocument(@RequestParam("title") String title, @RequestParam("description") String description,
			@RequestParam("category") String category, @RequestParam("file") MultipartFile file, HttpSession session)
			throws IOException {

		ReadOnline doc = new ReadOnline();
		doc.setTitle(title);
		doc.setDescription(description);
		doc.setCategory(category);

		if (!file.isEmpty()) {
			// Create directory if not exists
			File uploadDir = new File("src/main/resources/static/pdf");
			if (!uploadDir.exists()) {
				uploadDir.mkdirs();
			}

			// Generate unique filename
			String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
			doc.setFile(fileName);

			// Save file
			Path path = Paths.get(uploadDir.getAbsolutePath() + File.separator + fileName);
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
		}

		ReadOnline savedDoc = readOnlineService.saveDocument(doc);

		if (savedDoc != null) {
			session.setAttribute("succMsg", "Document saved successfully");
		} else {
			session.setAttribute("errorMsg", "Failed to save document");
		}

		return "redirect:/admin/library";
	}

	@GetMapping("/delete-document/{id}")
	public String deleteDocument(@PathVariable int id, HttpSession session) {
		readOnlineService.deleteDocument(id);
		session.setAttribute("succMsg", "Document deleted successfully");
		return "redirect:/admin/library";
	}

	@GetMapping("/edit-document/{id}")
	public String showEditDocument(@PathVariable int id, Model model) {
		ReadOnline document = readOnlineService.getDocumentById(id);
		model.addAttribute("document", document);
		model.addAttribute("categories", categoryService.getAllActiveCategory());
		return "admin/edit_library";
	}

	@PostMapping("/update-document")
	public String updateDocument(@RequestParam("id") int id, @RequestParam("title") String title,
			@RequestParam("description") String description, @RequestParam("category") String category,
			@RequestParam(value = "file", required = false) MultipartFile file, HttpSession session)
			throws IOException {

		ReadOnline existingDoc = readOnlineService.getDocumentById(id);

		// Update fields
		existingDoc.setTitle(title);
		existingDoc.setDescription(description);
		existingDoc.setCategory(category);

		// Handle file update
		if (file != null && !file.isEmpty()) {
			// Get the upload directory path from resources/static/pdf
			File uploadDir = new ClassPathResource("static/pdf").getFile();

			// Delete old file if exists
			if (existingDoc.getFile() != null && !existingDoc.getFile().isEmpty()) {
				Path oldFilePath = Paths.get(uploadDir.getAbsolutePath() + File.separator + existingDoc.getFile());
				Files.deleteIfExists(oldFilePath);
			}

			// Generate unique filename
			String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
			existingDoc.setFile(fileName); // Update the document's filename

			// Save new file
			Path newFilePath = Paths.get(uploadDir.getAbsolutePath() + File.separator + fileName);
			Files.copy(file.getInputStream(), newFilePath, StandardCopyOption.REPLACE_EXISTING);
		}

		ReadOnline updatedDoc = readOnlineService.updateDocument(existingDoc);

		if (updatedDoc != null) {
			session.setAttribute("succMsg", "Document updated successfully");
		} else {
			session.setAttribute("errorMsg", "Failed to update document");
		}

		return "redirect:/admin/library";
	}
	
	
	
}
