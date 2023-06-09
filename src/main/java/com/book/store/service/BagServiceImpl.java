package com.book.store.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.book.store.dto.BagDTO;
import com.book.store.dto.BookDTO;
import com.book.store.dto.FindOrderDTO;
import com.book.store.mapper.BagMapper;

@Service
public class BagServiceImpl implements BagService{
	
	@Autowired
	private BagMapper bagMapper;

	@Override
	public int maxNum() throws Exception {
		return bagMapper.maxNum();
	}

	@Override
	public void insertData(BagDTO dto) throws Exception {
		bagMapper.insertData(dto);
	}

	@Override
	public List<BookDTO> getLists(String userId) throws Exception {
		return bagMapper.getLists(userId);
	}

	@Override
	public BookDTO getReadData(int seq_No) throws Exception {
		return bagMapper.getReadData(seq_No);
	}

	@Override
	public void deleteData(int seq_No,String userId) throws Exception {
		bagMapper.deleteData(seq_No,userId);
	}

	@Override
	public int findOG(String userId) throws Exception {
		
		return bagMapper.findOG(userId);
	}
	
	@Override
	public void insertOrderData(int orderId, String userId, int seq_No, int orderGroup) throws Exception {
		bagMapper.insertOrderData(orderId, userId, seq_No, orderGroup);
	}

	@Override
	public int findOrderCount(String userId) throws Exception {
		return bagMapper.findOrderCount(userId);
	}

	@Override
	public List<FindOrderDTO> findAllOrders(String userId) throws Exception {
		return bagMapper.findAllOrders(userId);
	}

	@Override
	public String findOrderTitle(int orderId) throws Exception {
		return bagMapper.findOrderTitle(orderId);
	}

	@Override
	public int findOrderSu(String userId, int orderId) throws Exception {
		return bagMapper.findOrderSu(userId, orderId);
	}

	@Override
	public List<BookDTO> findOrderGroup(String userId, int orderId) throws Exception {
		return bagMapper.findOrderGroup(userId, orderId);
	}

	

	
	
}
