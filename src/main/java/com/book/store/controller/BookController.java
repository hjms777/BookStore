package com.book.store.controller;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.Session;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.book.store.dto.BookDTO;
import com.book.store.service.BookItemService;
import com.book.store.service.BookItemServiceImpl;
import com.book.store.user.UserData;
import com.book.store.util.FileManager;
import com.book.store.util.MyUtil;

//패키지 이름 com.book.store이여야만 함

@RestController
public class BookController {

	@Resource
	BookItemService bookItemService;
	@Resource
	HttpSession httpSession;

	@Autowired
	private MyUtil myUtil;

	@RequestMapping("/main")
	public ModelAndView home() throws Exception{
		//메인화면으로 이동
		ModelAndView mav = new ModelAndView();
		
		UserData user = null;
		if(httpSession.getAttribute("user") != null) {
			user = (UserData) httpSession.getAttribute("user");
			
		}else if(httpSession.getAttribute("OauthUser") != null) {
			user = (UserData) httpSession.getAttribute("OauthUser");
			if(user.getUserAddr()==null) {
				mav.setViewName("redirect:/user/oaumember");
				return mav;
			}
		}
		
//		//OAuth로 첫 로그인시 기타 회원정보 추가를 위해 가입페이지로 이동
//		if(user!=null && user.getUserAddr()==null) {
//		
//			mav.setViewName("redirect:/user/oaumember");
//			return mav;
//		}
		
		int start = 1;
		int end = 20;
		
		List<BookDTO> recentLists = bookItemService.recentLists(start, end);
		List<BookDTO> topSalLists = bookItemService.topSalLists(start, end);
		
		mav.addObject("recentLists", recentLists);
		mav.addObject("topSalLists", topSalLists);
		
		mav.setViewName("main");

		return mav;
	}
	

	@GetMapping("/BookCreated.action")
	public ModelAndView itemCreated() throws Exception{
		//신규상품 등록 홈페이지로 이동
		ModelAndView mav = new ModelAndView();

		mav.setViewName("BookCreated");

		return mav;

	}

	@PostMapping("/BookCreated.action")
	public ModelAndView itemCreated_ok(BookDTO dto,@RequestParam(value = "upload",required = false) List<MultipartFile> upload,
			HttpServletRequest request) throws Exception{
		//requestparam(value="html에서의 name", required 는 해당 매개변수가 필수면 true 아니면 false

		ModelAndView mav = new ModelAndView();
		
		int maxNum = bookItemService.maxNum();
		
		int backUpMaxNum = bookItemService.backUpMaxNum();
		
		if(backUpMaxNum >= maxNum) {
			
			dto.setSeq_No(backUpMaxNum+1);
			
		}else {
			
			dto.setSeq_No(maxNum+1);//일련번호 매기기
			
		}

		FileManager.doFileUpload(dto, upload);

		bookItemService.insertData(dto);

		mav.setViewName("redirect:BookList.action");

		return mav;

	}

	@GetMapping("/BookList.action")
	public ModelAndView list(HttpServletRequest request) throws Exception{
		
		String pageNum = request.getParameter("pageNum");

		int currentPage = 1; //첫화면은 1페이지 

		if(pageNum!=null) {

			currentPage = Integer.parseInt(pageNum);
			//1페이지가 아닌 get방식 주소로 받은 pageNum로 변경
		}

		String searchKey = request.getParameter("searchKey");
		//searchKey는 작가,제목,도서번호
		String searchValue = request.getParameter("searchValue");
		
		if(searchValue==null) {

			searchKey = "title_Nm";
			searchValue = "";

		}else {//get방식으로 오는거 대소문자 상관없이 searchValue를 utf-8로 디코드
			if(request.getMethod().equalsIgnoreCase("GET")) {
				searchValue = URLDecoder.decode(searchValue,"utf-8");
			}
		}

		int dataCount = bookItemService.getDataCount(searchKey, searchValue);
		//searchKey를 매개변수로 인식하지 못하는 현상 발행 - Mapper.java에 @Param을 붙여서 인식하게 만듬
		int numPerPage = 9;
		//한페이지에 9개의 아이템

		int totalPage = myUtil.getPageCount(numPerPage, dataCount);

		if(currentPage>totalPage) {
			currentPage=totalPage;
		}

		int start = (currentPage-1)*numPerPage+1;
		int end = currentPage*numPerPage;

		List<BookDTO> lists = bookItemService.getLists(start, end, searchKey,searchValue);
		//Mapper.xml에서 TO_CHAR부분 에러발생 데이터 형식이 이미 2023-04-05형태여서 그런듯

		for (int i = lists.size(); i < numPerPage; i++) {

			lists.add(null);
			//list칸수 맞출려고 강제로 null값 주입
		}

		/*

		// 제목이 너무 길어서 css가 깨지길래 사이에<br/>해보기
		//css width 너비 30%로 고정해서 막음

		Iterator<BookDTO> it = lists.iterator();

		while(it.hasNext()) {

			BookDTO dto = it.next();

			String title = dto.getTitle_Nm();

			if(title.length()>=15) {

				StringBuffer sb = new StringBuffer();

				sb.append(title);

				sb.insert(14, "<br/>");
				//중간에 문자삽입 가능하게 만들어줌

				dto.setTitle_Nm(sb.toString());

				System.out.println(dto.getTitle_Nm());

				//th:utext써서 태그까지 출력하게 하려고함

			}

		}
		 */

		String param = ""; if(searchValue!=null&&!searchValue.equals("")) { param =
				"searchKey=" + searchKey; param+= "&searchValue=" +
						URLEncoder.encode(searchValue,"utf-8"); }

		String listUrl = "/BookList.action";

		if(!param.equals("")) { listUrl += "?" + param; }

		String pageIndexList = myUtil.pageIndexList(currentPage, totalPage, listUrl);

		String articleUrl = "/BookArticle.action?pageNum=" + currentPage;

		if(!param.equals("")) { articleUrl += "&" + param; }

		ModelAndView mav = new ModelAndView();

		mav.addObject("lists", lists); 
		mav.addObject("pageIndexList", pageIndexList);
		mav.addObject("articleUrl",articleUrl); 
		mav.addObject("pageNum", currentPage);
		
		//통합검색 결과 갯수 확인
		mav.addObject("dataCount", dataCount); 
		mav.addObject("searchValue", searchValue); 
		//이걸로 통합검색 창 뜨게할지 안 할지 확인
		mav.addObject("searchKey", searchKey); 

		
		mav.setViewName("BookList");
		//진짜 주소로 가서 이걸 뿌려줘야 함

		return mav;
	}

	@GetMapping("/BookArticle.action")
	public ModelAndView article(HttpServletRequest request) throws Exception{

		ModelAndView mav = new ModelAndView();
		
		UserData user = null;
		if(httpSession.getAttribute("user") != null) {
			user = (UserData) httpSession.getAttribute("user");
			mav.addObject("userId", user.getUserId());
			mav.addObject("RULE", user.getUserRole());
		}else if(httpSession.getAttribute("OauthUser") != null) {
			user = (UserData) httpSession.getAttribute("OauthUser");
			mav.addObject("userId", user.getUserId());
			mav.addObject("RULE", user.getUserRole());
		}
		int seq_No = Integer.parseInt(request.getParameter("seq_No"));

		String pageNum = request.getParameter("pageNum");
		
		//pageNum을 무조건 받아야 article로 들어오는데 주소에 &pageNum 일일이 쓰기 귀찮아서 이렇게 함
		if(pageNum==null) {

			pageNum = "1";
			Integer.parseInt(pageNum);
			
		}else {
			
			Integer.parseInt(pageNum);
			
		}
		
		String searchKey = request.getParameter("searchKey");
		String searchValue = request.getParameter("searchValue");

		if(searchValue!=null) {
			searchValue = URLDecoder.decode(searchValue,"utf-8");
		}

		BookDTO dto = bookItemService.getReadData(seq_No);
		
		if(dto==null) {

			mav.setViewName("redirect:BookList.action?pageNum=" + pageNum 
					+ "&searchKey=" + searchKey + "&searchValue=" + searchValue);

			return mav;

		}

		String param = "pageNum=" + pageNum;

		if(searchValue!=null && !searchValue.equals("")) {
			param += "&searchKey=" + searchKey;
			param += "&searchValue=" + URLEncoder.encode(searchValue, "utf-8");
		}


		mav.addObject("dto", dto);
		mav.addObject("params", param);
		mav.addObject("pageNum", pageNum);
		
		mav.setViewName("product");

		return mav;

	}

	@GetMapping("/BookUpdate.action")
	public ModelAndView updated(HttpServletRequest request) throws Exception{

		int seq_No = Integer.parseInt(request.getParameter("seq_No"));
		String pageNum = request.getParameter("pageNum");

		String searchKey = request.getParameter("searchKey");
		String searchValue = request.getParameter("searchValue");

		if(searchValue!=null) {
			searchValue = URLDecoder.decode(searchValue,"utf-8");
		}

		BookDTO dto = bookItemService.getReadData(seq_No);

		if(dto==null) {
			ModelAndView mav = new ModelAndView();
			mav.setViewName("redirect:BookList.action?pageNum=" + pageNum 
					+ "&searchKey=" + searchKey + "&searchValue=" + searchValue);
			return mav;
		}

		String param = "pageNum=" + pageNum;

		if(searchValue!=null && !searchValue.equals("")) {
			param += "&searchKey=" + searchKey;
			param += "&searchValue=" + URLEncoder.encode(searchValue, "utf-8");
		}

		ModelAndView mav = new ModelAndView();

		mav.addObject("dto", dto);
		mav.addObject("pageNum", pageNum);
		mav.addObject("params", param);
		mav.addObject("searchKey", searchKey);
		mav.addObject("searchValue", searchValue);

		mav.setViewName("BookUpdate");

		return mav;

	}

	@PostMapping("/BookUpdate.action")
	public ModelAndView updated_ok(HttpServletRequest request,BookDTO dto,
			@RequestParam(value = "upload",required = false) List<MultipartFile> upload) throws Exception{
		//MultipartFile는 받을 때 List 형식으로 받아줘야함
		
		String pageNum = request.getParameter("pageNum");
		String searchKey = request.getParameter("searchKey");
		String searchValue = request.getParameter("searchValue");
		String image_Url = request.getParameter("image_Url");
		
		//upload된게 없으면 그대로 두고 있으면 지워야됨
		//multipart는 list에서 하나씩 꺼내서 null값을 체크해줘야 함
		int checkEmpty = 1;
		
		for(MultipartFile image:upload){
			
			if(image.isEmpty()) {
				
				checkEmpty = 0;
				
			}
		}
		
		if(checkEmpty != 0) {
			
			FileManager.doFileDelete(image_Url);
			FileManager.doFileUpload(dto, upload);
			
		}else {
			
			dto.setImage_Url(image_Url);
			
		}
		
		bookItemService.updateData(dto);

		String param = "pageNum=" + pageNum;

		if(searchValue!=null && !searchValue.equals("")) {
			param += "&searchKey=" + searchKey;
			param += "&searchValue=" + URLEncoder.encode(searchValue, "utf-8");
		}


		ModelAndView mav = new ModelAndView();

		mav.setViewName("redirect:BookList.action?" + param);

		return mav;

	}

	@GetMapping("/BookDelete.action")
	public ModelAndView deleted_ok(HttpServletRequest request) throws Exception{

		//이거 upload폴더에 있는 파일도 같이 지워야됨
		int seq_No = Integer.parseInt(request.getParameter("seq_No"));
		String pageNum = request.getParameter("pageNum");
		String searchKey = request.getParameter("searchKey");
		String searchValue = request.getParameter("searchValue");
		String image_Url = request.getParameter("image_Url");

		BookDTO dto = bookItemService.getReadData(seq_No);
		
		//백업테이블에 seq_No중 최대 번호 입력
		int seq_Max = bookItemService.maxNum();
		
		dto.setSeq_Max(seq_Max);
		
		//백업테이블에 백업
		bookItemService.insertBackUp(dto);
		
		bookItemService.deleteData(seq_No);

		FileManager.doFileDelete(image_Url);

		String param = "pageNum=" + pageNum;

		if(searchValue!=null && !searchValue.equals("")) {
			param += "&searchKey=" + searchKey;
			param += "&searchValue=" + URLEncoder.encode(searchValue, "utf-8");
		}

		ModelAndView mav = new ModelAndView();

		mav.setViewName("redirect:BookList.action?" + param);

		return mav;

	}
	
	@GetMapping("/BookCategoryList.action")
	public ModelAndView categoryList(HttpServletRequest request) throws Exception{
		
		ModelAndView mav = new ModelAndView();
		
		UserData user = null;
		if(httpSession.getAttribute("user") != null) {
			user = (UserData) httpSession.getAttribute("user");
			mav.addObject("RULE", user.getUserRole());
		}else if(httpSession.getAttribute("OauthUser") != null) {
			user = (UserData) httpSession.getAttribute("OauthUser");
			mav.addObject("RULE", user.getUserRole());
		}
		
		String pageNum = request.getParameter("pageNum");

		int currentPage = 1; //첫화면은 1페이지 

		if(pageNum!=null) {

			currentPage = Integer.parseInt(pageNum);
			//1페이지가 아닌 get방식 주소로 받은 pageNum로 변경
		}

		String searchKey = request.getParameter("searchKey");
		//searchKey는 작가,제목,도서번호
		String searchValue = request.getParameter("searchValue");

		if(searchValue==null || searchValue.equals("") || searchValue == "") {

			searchKey = "title_Nm";
			searchValue = "";

		}else {//get방식으로 오는거 대소문자 상관없이 searchValue를 utf-8로 디코드
			if(request.getMethod().equalsIgnoreCase("GET")) {
				searchValue = URLDecoder.decode(searchValue,"utf-8");
			}
			
			//kdc_Nm 첫 숫자를 뽑아 카테고리 구분
			searchValue = searchValue.substring(0, 1);
			
		}

		int dataCount = bookItemService.getDataCount(searchKey, searchValue);
		//searchKey를 매개변수로 인식하지 못하는 현상 발행 - Mapper.java에 @Param을 붙여서 인식하게 만듬
		int numPerPage = 9;
		//한페이지에 9개의 아이템

		int totalPage = myUtil.getPageCount(numPerPage, dataCount);

		if(currentPage>totalPage) {
			currentPage=totalPage;
		}

		int start = (currentPage-1)*numPerPage+1;
		int end = currentPage*numPerPage;

		List<BookDTO> lists = bookItemService.categoryLists(start, end, searchKey, searchValue);

		for (int i = lists.size(); i < numPerPage; i++) {

			lists.add(null);
			//list칸수 맞출려고 강제로 null값 주입
		}

		String param = ""; if(searchValue!=null&&!searchValue.equals("")) { param =
				"searchKey=" + searchKey; param+= "&searchValue=" +
						URLEncoder.encode(searchValue,"utf-8"); }

		String listUrl = "/BookCategoryList.action";

		if(!param.equals("")) { listUrl += "?" + param; }

		String pageIndexList = myUtil.pageIndexList(currentPage, totalPage, listUrl);

		String articleUrl = "/BookArticle.action?pageNum=" + currentPage;

		if(!param.equals("")) { articleUrl += "&" + param; }


		mav.addObject("lists", lists); 
		mav.addObject("pageIndexList", pageIndexList);
		mav.addObject("articleUrl",articleUrl); 
		mav.addObject("pageNum", currentPage);
		
		//통합검색 결과 갯수 확인
		mav.addObject("dataCount", dataCount); 
		mav.addObject("searchValue", searchValue); 
		
		//이걸로 통합검색 창 뜨게할지 안 할지 확인
		mav.addObject("searchKey", searchKey); 

		
		mav.setViewName("BookList");
		//진짜 주소로 가서 이걸 뿌려줘야 함

		return mav;
		
	}
	
		//신작
		@GetMapping("/NewBook.action")
		public ModelAndView newlist(HttpServletRequest request) throws Exception{

			String pageNum = request.getParameter("pageNum");

			int currentPage = 1; //첫화면은 1페이지 

			if(pageNum!=null) {

				currentPage = Integer.parseInt(pageNum);
				//1페이지가 아닌 get방식 주소로 받은 pageNum로 변경
			}

			String searchKey = request.getParameter("searchKey");
			//searchKey는 작가,제목,도서번호
			String searchValue = request.getParameter("searchValue");

			if(searchValue==null) {

				searchKey = "title_Nm";
				searchValue = "";

			}else {//get방식으로 오는거 대소문자 상관없이 searchValue를 utf-8로 디코드
				if(request.getMethod().equalsIgnoreCase("GET")) {
					searchValue = URLDecoder.decode(searchValue,"utf-8");
				}
			}

			int dataCount = bookItemService.getDataCount(searchKey, searchValue);
			//searchKey를 매개변수로 인식하지 못하는 현상 발행 - Mapper.java에 @Param을 붙여서 인식하게 만듬
			int numPerPage = 9;
			//한페이지에 9개의 아이템

			int totalPage = myUtil.getPageCount(numPerPage, 20);

			if(currentPage>totalPage) {
				currentPage=totalPage;
			}

			int start = (currentPage-1)*numPerPage + 1;
			int end = currentPage * numPerPage;

			if (end > 20) { // 마지막 페이지일 경우
			  end = 20;
			}

			List<BookDTO> lists = bookItemService.recentLists(start, end);
			//Mapper.xml에서 TO_CHAR부분 에러발생 데이터 형식이 이미 2023-04-05형태여서 그런듯

			for (int i = lists.size(); i < numPerPage; i++) {

				lists.add(null);
				//list칸수 맞출려고 강제로 null값 주입
			}

			String param = ""; if(searchValue!=null&&!searchValue.equals("")) { param =
					"searchKey=" + searchKey; param+= "&searchValue=" +
							URLEncoder.encode(searchValue,"utf-8"); }

			String listUrl = "/NewBook.action";

			if(!param.equals("")) { listUrl += "?" + param; }

			String pageIndexList = myUtil.pageIndexList(currentPage, totalPage, listUrl);

			String articleUrl = "/BookArticle.action?pageNum=" + currentPage;

			if(!param.equals("")) { articleUrl += "&" + param; }

			ModelAndView mav = new ModelAndView();

			mav.addObject("lists", lists); 
			mav.addObject("pageIndexList", pageIndexList);
			mav.addObject("dataCount", dataCount); 
			mav.addObject("articleUrl",articleUrl); 
			mav.addObject("pageNum", currentPage);

			mav.setViewName("BookList");
			//진짜 주소로 가서 이걸 뿌려줘야 함

			return mav;
		
		}
		
		//베스트셀러
		@GetMapping("/BestBook.action")
		public ModelAndView bestlist(HttpServletRequest request) throws Exception{
			String pageNum = request.getParameter("pageNum");

			int currentPage = 1; //첫화면은 1페이지 

			if(pageNum!=null) {

				currentPage = Integer.parseInt(pageNum);
				//1페이지가 아닌 get방식 주소로 받은 pageNum로 변경
			}

			String searchKey = request.getParameter("searchKey");
			//searchKey는 작가,제목,도서번호
			String searchValue = request.getParameter("searchValue");

			if(searchValue==null) {

				searchKey = "title_Nm";
				searchValue = "";

			}else {//get방식으로 오는거 대소문자 상관없이 searchValue를 utf-8로 디코드
				if(request.getMethod().equalsIgnoreCase("GET")) {
					searchValue = URLDecoder.decode(searchValue,"utf-8");
				}
			}

			int dataCount = bookItemService.getDataCount(searchKey, searchValue);
			//searchKey를 매개변수로 인식하지 못하는 현상 발행 - Mapper.java에 @Param을 붙여서 인식하게 만듬
			int numPerPage = 9;
			//한페이지에 9개의 아이템

			int totalPage = myUtil.getPageCount(numPerPage, 20);

			if(currentPage>totalPage) {
				currentPage=totalPage;
			}

			int start = (currentPage-1)*numPerPage+1;
			int end = currentPage*numPerPage;

			if (end > 20) { // 마지막 페이지일 경우
			  end = 20;
			}

			List<BookDTO> lists = bookItemService.topSalLists(start, end);
			//Mapper.xml에서 TO_CHAR부분 에러발생 데이터 형식이 이미 2023-04-05형태여서 그런듯

			for (int i = lists.size(); i < numPerPage; i++) {

				lists.add(null);
				//list칸수 맞출려고 강제로 null값 주입
			}

			String param = ""; if(searchValue!=null&&!searchValue.equals("")) { param =
					"searchKey=" + searchKey; param+= "&searchValue=" +
							URLEncoder.encode(searchValue,"utf-8"); }

			String listUrl = "/BestBook.action";

			if(!param.equals("")) { listUrl += "?" + param; }

			String pageIndexList = myUtil.pageIndexList(currentPage, totalPage, listUrl);

			String articleUrl = "/BookArticle.action?pageNum=" + currentPage;

			if(!param.equals("")) { articleUrl += "&" + param; }

			ModelAndView mav = new ModelAndView();

			mav.addObject("lists", lists); 
			mav.addObject("pageIndexList", pageIndexList);
			mav.addObject("dataCount", dataCount); 
			mav.addObject("articleUrl",articleUrl); 
			mav.addObject("pageNum", currentPage);

			mav.setViewName("BookList");
			//진짜 주소로 가서 이걸 뿌려줘야 함

			return mav;
		
		}	
	

}
