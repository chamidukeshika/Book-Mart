package com.ecom.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.model.Cart;
import com.ecom.model.OrderAddress;
import com.ecom.model.OrderRequest;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.service.OrderService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private ProductOrderRepository orderRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CommonUtil commonUtil;

	@Override
	@Transactional
	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception {
		List<Cart> carts = cartRepository.findByUserId(userid);
		List<ProductOrder> orders = new ArrayList<>();

		for (Cart cart : carts) {
			Product product = cart.getProduct();
			int quantity = cart.getQuantity();

			// Deduct stock first
			int updated = productRepository.deductStock(product.getId(), quantity);
			if (updated == 0) {
				throw new Exception("Insufficient stock for product: " + product.getTitle());
			}

			// Fetch latest product data after stock deduction
			Product updatedProduct = productRepository.findById(product.getId())
					.orElseThrow(() -> new Exception("Product not found"));

			updatedProduct.setStock(updatedProduct.getStock());

			// Check if low stock notification needs to be sent
			if (updatedProduct.getStock() < 10 && !updatedProduct.isLowStockNotificationSent()) {
				commonUtil.sendLowStockEmail(updatedProduct);
				// commonUtil.sendLowStockSms(updatedProduct);
				// commonUtil.sendLowStockVoiceAlert(updatedProduct);

				updatedProduct.setLowStockNotificationSent(true);
			}

			productRepository.save(updatedProduct);

			// Create and save order
			ProductOrder order = new ProductOrder();
			order.setOrderId(UUID.randomUUID().toString());
			order.setOrderDate(new Date());
			order.setProduct(updatedProduct);
			order.setPrice(updatedProduct.getDiscountPrice());
			order.setQuantity(quantity);
			order.setUser(cart.getUser());
			order.setStatus(OrderStatus.IN_PROGRESS.getName());
			order.setPaymentType(orderRequest.getPaymentType());

			OrderAddress address = new OrderAddress();
			address.setFirstName(orderRequest.getFirstName());
			address.setLastName(orderRequest.getLastName());
			address.setEmail(orderRequest.getEmail());
			address.setMobileNo(orderRequest.getMobileNo());
			address.setAddress(orderRequest.getAddress());
			address.setCity(orderRequest.getCity());
			address.setState(orderRequest.getState());
			address.setPincode(orderRequest.getPincode());

			order.setOrderAddress(address);

			ProductOrder savedOrder = orderRepository.save(order);
			orders.add(savedOrder);
		}

		// Send one email with all order details
		commonUtil.sendMailForAllProductOrders(orders, "Success");
	}


	
	@Override
	public List<ProductOrder> getOrdersByUser(Integer userId) {

		List<ProductOrder> orders = orderRepository.findByUserId(userId);

		return orders;
	}

	@Override
	public ProductOrder updateOrderStatus(Integer id, String status) {

		Optional<ProductOrder> findByid = orderRepository.findById(id);

		if (findByid.isPresent()) {

			ProductOrder productOrders = findByid.get();
			productOrders.setStatus(status);
			ProductOrder updateOrder = orderRepository.save(productOrders);
			return updateOrder;

		}

		return null;
	}

	@Override
	public List<ProductOrder> getAllOrders() {

		return orderRepository.findAll();
	}

	@Override
	public ProductOrder getOrderByOrderId(String orderId) {

		return orderRepository.findByOrderId(orderId);
	}

	@Override
	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {

		Pageable pageable = PageRequest.of(pageNo, pageSize);

		return orderRepository.findAll(pageable);
	}

	@Override
	public Page<ProductOrder> getAllOrdersPagination(Pageable pageable) {
		return orderRepository.findAll(pageable);
	}

}
