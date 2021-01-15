package com.kosmo.k11mybatis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import mybatis.MemberVO;
import mybatis.MyBoardDTO;
import mybatis.MybatisDAOImpl;
import mybatis.MybatisMemberImpl;
import mybatis.ParameterDTO;
import util.PagingUtil;

@Controller
public class JSONController {
	
	//Mybatis를 사용하기 위한 빈을 자동주입받음.
	@Autowired
	private SqlSession sqlSession;
	
	//방명록 게시판의 틀이 되는 페이지
	@RequestMapping("/mybatisJSON/list.do")
	public String board() {
		return "08Json/board";
	}
	
	@RequestMapping("/mybatisJSON/aList.do")
	public String aList(Model model, HttpServletRequest req) {
		
		//파라미터 저장을 위한 DTO객체 생성
		ParameterDTO parameterDTO = new ParameterDTO();
		parameterDTO.setSearchField(req.getParameter("searchField"));
		
		ArrayList<String> searchLists = null;
		if(req.getParameter("searchTxt")!=null) {
			searchLists = new ArrayList<String>();
			//검색어는 스페이스로 구분되므로 split()을 통해 문자열배열로 반환한다.
			String[] sTxtArray = req.getParameter("searchTxt").split(" ");
			//배열의 크기만큼 반복하면서 컬렉션에 추가한다
			for(String str : sTxtArray) {
				searchLists.add(str);
			}
		}
		parameterDTO.setSearchTxt(searchLists);
		System.out.println("검색어 : "+ parameterDTO.getSearchTxt());
		
		//게시물의 갯수를 카운트.
		/*
		서비스객체 역할을 하는 인터페이스에 정의된 추상메소드를 호출하면
		최종적으로 Mapper의 동일한 id속성값을 가진 엘리먼트의 쿼리문이
		실행되어 결과를 반환받게 된다.
		sqlSession.getMapper(서비스객체)로 함수실행 
		 */
		int totalRecordCount = 
				sqlSession.getMapper(MybatisDAOImpl.class).getTotalCount(parameterDTO);
		//페이지 처리를 위한 설정값
		int pageSize = 4;
		int blockPage = 2;
		//전체페이지수 계산
		int totalPage = (int)Math.ceil((double)totalRecordCount/pageSize);
		
		//현재페이지 번호 가져오기
		int nowPage = req.getParameter("nowPage")==null? 1:
			Integer.parseInt(req.getParameter("nowPage"));
		//select할 게시물의 구간을 계산
		int start = (nowPage-1)*pageSize + 1;
		int end = nowPage * pageSize;
		//기존의 형태와는 다르게 DTO객체에 저장한 후 Mapper를 호출한다.
		parameterDTO.setStart(start);
		parameterDTO.setEnd(end);
		
		//Mapper 호출
		ArrayList<MyBoardDTO> lists = sqlSession.getMapper(MybatisDAOImpl.class)
				.listPage(parameterDTO);
		//MyBatis 기본쿼리 출력
        String sql = sqlSession.getConfiguration().getMappedStatement("listPage").getBoundSql(parameterDTO).getSql();
        System.out.println("sql="+sql);
		//페이지번호 처리 
		String pagingImg = 
				PagingUtil.pagingAjax(totalRecordCount, pageSize, blockPage, nowPage,
						req.getContextPath()+"/mybatis/list.do?");
		
		model.addAttribute("pagingImg", pagingImg);
		//게시물의 줄바꿈 처리
		for(MyBoardDTO dto : lists) {
			String temp = 
					dto.getContents().replace("\r\n", "<br/>");
			dto.setContents(temp);
		}
		
		model.addAttribute("lists", lists);
		return "08Json/aList";
	}
	//로그인페이지
	@RequestMapping("/mybatisJSON/login.do")
	public String login() {
		return "08Json/login";
	}
	//로그아웃 처리
	@RequestMapping("/mybatisJSON/logout.do")
	public String logout(HttpSession session) {
		session.removeAttribute("siteUserInfo");
		return "redirect:list.do";
	}
	//로그인 처리 부분
	@RequestMapping("/mybatisJSON/loginAction.do")
	@ResponseBody
	public Map<String, Object> loginAction(HttpServletRequest req, HttpSession session) {
		
		//콜백데이터로  JSON을 사용하기 위해 Map컬렉션 선언
		Map<String, Object> map = new HashMap<String, Object>();
		
		//Mybatis를 통해 로그인 확인
		MemberVO vo = sqlSession.getMapper(MybatisMemberImpl.class).login(
				req.getParameter("id"), req.getParameter("pass"));
		
		if(vo==null) {
			//로그인에 실패한 경우.....
			map.put("loginResult", 0);
			map.put("loginMessage", "로그인 실패");
		}
		else {
			session.setAttribute("siteUserInfo", vo);
			map.put("loginResult", 1);
			map.put("loginMessage", "로그인 성공");
		}
		
		return map;
	}
	
	@RequestMapping("/mybatisJSON/deleteAction.do")
	@ResponseBody
	public Map<String, Object> deleteAction(HttpServletRequest req,
			HttpSession session){
		Map<String, Object> map = new HashMap<String, Object>();
		
		if(session.getAttribute("siteUserInfo")==null) {
			//로그인이 안된경우라면 반환코드 1
			map.put("statusCode", 1);
			return map;
		}
		
		int result = sqlSession.getMapper(MybatisDAOImpl.class).delete(
				req.getParameter("idx"), 
				((MemberVO)session.getAttribute("siteUserInfo")).getId());
		
		if(result<=0) {
			//삭제실패라면 반환코드 0
			map.put("statusCode", 0);
		}
		else {
			//삭제성공이면 반환코드 2
			map.put("statusCode", 2);
		}
		return map;
	}
}
