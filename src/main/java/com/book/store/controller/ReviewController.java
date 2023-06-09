package com.book.store.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.book.store.dto.ReviewDTO;
import com.book.store.service.BookItemService;
import com.book.store.service.ReviewService;
import com.book.store.service.UserService;
import com.book.store.user.UserData;
import com.book.store.util.BoardUtil;
import com.book.store.util.MyUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class ReviewController {

	private final HttpSession httpSession;
	
	@Resource
	private ReviewService reviewService; //reviewServiceImpl 호출

	@Resource
	private BookItemService bookItemService;
	
	@Resource
	private UserService userService;

	@Autowired
	BoardUtil myUtil;//아작스 페이징
	
	@Autowired
	MyUtil myUtil2;//페이징


	@PostMapping("/ReviewCreated.action")
	public ModelAndView created(HttpServletRequest request,ReviewDTO dto) throws Exception{

		int seq_No = Integer.parseInt(request.getParameter("seq_No"));
		String userId = dto.getUserId();
		
		int vip = userService.findUserVip(userId);
		String userVip = "";
		
		if(vip>=40) {
			
			userVip = "Diamond";
			
		}else if(vip>=30 && vip<40) {
			
			userVip = "Gold";
			
		}else if(vip>=20 && vip<30) {
			
			userVip = "Silver";
			
		}else if(vip>=10 && vip<20) {
			
			userVip = "Bronze";
			
		}else {
			
			userVip = "NewBie";
			
		}
		
		ReviewDTO checkId = reviewService.checkUserId(seq_No, userId);
		//mapper에 review테이블에 userId가 있는지 확인
		
		if(checkId != null) {
		//있으면 get방식으로 체크할 변수 보냄
			
			ModelAndView mav = new ModelAndView();
			mav.setViewName("redirect:/ReviewList.action?seq_No=" + seq_No + "&checkId=checking");
			return mav;
			
		}
		
		int maxNum = reviewService.maxNum();

		dto.setReviewId(maxNum+1);
		dto.setIpAddr(request.getRemoteAddr());
		dto.setVip(userVip);
		
		reviewService.insertData(dto);

		ModelAndView mav = new ModelAndView();

		mav.setViewName("redirect:/ReviewList.action?seq_No=" + seq_No);

		return mav;
	}

	@RequestMapping("/ReviewList.action")
	public ModelAndView list(HttpServletRequest request,ReviewDTO dto) throws Exception{

		int seq_No = Integer.parseInt(request.getParameter("seq_No"));
				
		String checkId = request.getParameter("checkId");
		
		String pageNum = request.getParameter("pageNum");

		int currentPage = 1; //첫화면은 1페이지 

		if(pageNum!=null) {

			currentPage = Integer.parseInt(pageNum);
			//1페이지가 아닌 get방식 주소로 받은 pageNum로 변경
		}

		int dataCount = reviewService.getDataCount(seq_No);

		int numPerPage = 5;

		int totalPage = myUtil.getPageCount(numPerPage, dataCount);

		if(currentPage>totalPage) {
			currentPage=totalPage;
		}

		int start = (currentPage-1)*numPerPage+1;
		int end = currentPage*numPerPage;

		List<ReviewDTO> lists = reviewService.getLists(start, end, seq_No);
		
		double avgStar = reviewService.getAvgStar(seq_No);

		String pageIndexList = myUtil.pageIndexList(currentPage, totalPage);

		ModelAndView mav = new ModelAndView();

		mav.addObject("lists", lists);
		mav.addObject("pageNum", currentPage);
		mav.addObject("pageIndexList", pageIndexList);
		mav.addObject("checkId", checkId);
		mav.addObject("avgStar", avgStar);
		mav.addObject("dataCount", dataCount);

		mav.setViewName("commentList");

		return mav;
	}

	@PostMapping("ReviewDelte.action")
	public ModelAndView delete(HttpServletRequest request,ReviewDTO dto) throws Exception{

		String pageNum = request.getParameter("pageNum");

		//pageNum을 무조건 받아야 article로 들어오는데 주소에 &pageNum 일일이 쓰기 귀찮아서 이렇게 함
		if(pageNum==null) {

			pageNum = "1";
			Integer.parseInt(pageNum);

		}else {

			Integer.parseInt(pageNum);

		}

		int reviewId = dto.getReviewId();

		int seq_No = dto.getSeq_No();

		reviewService.deleteData(reviewId);
		
		String param = "seq_No=" + seq_No + "&pageNum=" + pageNum;

		ModelAndView mav = new ModelAndView();

		mav.setViewName("redirect:/ReviewList.action?" + param);

		return mav;

	}
	
	@RequestMapping("userReview.action")
	public ModelAndView userReview(HttpServletRequest request) throws Exception{
		
		UserData user = null;

		if(httpSession.getAttribute("user")!=null) {

			user = (UserData) httpSession.getAttribute("user");


		}else if(httpSession.getAttribute("OauthUser")!=null) {

			user = (UserData) httpSession.getAttribute("OauthUser");

		}
		
		String userId = user.getUserId();
		
		int numPerPage = 2;
		
		int dataCount = reviewService.getReviewCount(userId);

		List<ReviewDTO> lists = reviewService.userReview(1, numPerPage, userId);
	
		ModelAndView mav = new ModelAndView();

		mav.addObject("lists", lists);
		mav.addObject("dataCount", dataCount);
		mav.addObject("numPerPage", numPerPage);
		
		mav.setViewName("userReviewLists");

		return mav;
	}
	

}
