<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta http-equiv="refresh" content="60">
	<link href="resources/css/bootstrap.min.css" rel='stylesheet' type='text/css' />
	<link href="resources/css/style.css" rel='stylesheet' type='text/css' />
	<link href="resources/css/font-awesome.css" rel="stylesheet"> 
	<link rel="stylesheet" type="text/css" href="resources/cosbench.css" />
	<script src="resources/js/jquery-1.10.2.min.js"></script>
	
  <script type="text/javascript">
	function checkAll(event, str) {
		var e = window.event ? window.event.srcElement : event.currentTarget;
		var a = document.getElementsByName(str);
		for ( var i = 0; i < a.length; i++)
			a[i].checked = e.checked;
	}
	function checkItem(event, allId) {
		var e = window.event ? window.event.srcElement : event.currentTarget;
		var all = document.getElementById(allId);
		check(e, all);
	}
	function checkMe(id, allId) {
		document.getElementById('checkbox-' + id).checked = !document
				.getElementById('checkbox-' + id).checked;
		var all = document.getElementById(allId);
		check(document.getElementById('checkbox-' + id), all);
	}
	function check(item, all) {
		if (item.checked) {
			var a = document.getElementsByName(item.name);
			all.checked = true;
			for ( var i = 0; i < a.length; i++) {
				if (!a[i].checked) {
					all.checked = false;
					break;
				}
			}
		} else
			all.checked = false;
	}
	function findChecked(name) {
		var a = document.getElementsByName(name);
		var value = '';
		for ( var i = 0; i < a.length; i++) {
			if (a[i].checked) {
				value += a[i].value;
				value += "_";
			}
		}
		return value.substring(0, value.length - 1);
	}
	function changeOrder(id, value) {
		var neighid = findChecked('ActiveWorkload');
		if (neighid.indexOf("_") != -1) {
			neighid = 0;
		}
		document.getElementById('neighid-' + id).value = neighid;
		document.getElementById('up-' + id).value = value;
	}
  </script>
  <title>COSBench Controller</title> 
</head>
<body>
	<#include "navigationBar.ftl">
		
	<div id="rightContent">
		<#include "header.ftl">
		<div id="main">
			<div class="top"><br /></div>
			<div class="content">
			    
			  <h3>All users<span class="counter state">${totalUsers}</span></h3>
			  <div>
				<table class="info-table">
				  <tr>
					<th style="width:5%;"><input type="checkbox" id="AllActive" onclick="checkAll(event,'User')"></th>
					<th class="id" style="width:5%;">ID</th>
					<th style="width:20%;">UserName</th>
					<th style="width:20%;">UserGroup</th>
					<th style="width:50%;">Description</th>
				  </tr>
				  <#list users as user >
					<#if highlightId?? && user.id == highlightId >
						<tr class="high-light">
					<#else>
					  <tr>
					</#if>
					  <td><input type="checkbox" id="checkbox-${user.id}" name="User" onclick="checkItem(event,'AllActive')" value="${user.id}"></td>
					  <td onclick="checkMe('${user.id}','AllActive');">${user.id}</td>
					  <td onclick="checkMe('${user.id}','AllActive');">${user.userName}</td>
					  <td onclick="checkMe('${user.id}','AllActive');">${user.userGroup}</td>
					  <td onclick="checkMe('${user.id}','AllActive');">${user.description}</td>
					</tr>
				  </#list>
				</table>
			  </div>
			  <table class="info-table">
				<tr>
				  	<td style="width:50%;"></td>
				  	<td style="width:50%;">
				  	<h>TotalPage:&nbsp;</h><span id = "totalPage">${totalPage}</span>
				  	<h>&nbsp;&nbsp;CurrentPage:&nbsp;</h><span id = "currentPage">${currentPage}</span>
				  	<a href="userManagement.html?page=1">&nbsp;&nbsp;FirstPage</a>&nbsp;&nbsp;&nbsp;&nbsp;
				  	<#if (currentPage-1 >= 1)>
				  	<a href="userManagement.html?page=${currentPage-1}">Previous</a>&nbsp;&nbsp;&nbsp;&nbsp;
				  	<#else>
				  	<a href="javascript:void(0)" disabled = 'true'  style="cursor: default;opacity: 0.6;">Previous</a>&nbsp;&nbsp;&nbsp;&nbsp;
				  	</#if>
				  	
				  	<#if (currentPage+1 <= totalPage)>
				  	<a href="userManagement.html?page=${currentPage+1}">Next</a>&nbsp;&nbsp;&nbsp;&nbsp;
				  	<#else>
				  	<a href="javascript:void(0)" disabled = 'true' style="cursor: default;opacity: 0.6;">Next</a>&nbsp;&nbsp;&nbsp;&nbsp;
				  	</#if>
				  	<a href="userManagement.html?page=${totalPage}">LastPage</a>&nbsp;&nbsp;&nbsp;&nbsp;
				  	</td>
				  </tr>
				</table>
				
			  <p style="margin-bottom:0px">
			  	<a class="label" href="download-userTemplet.do" style="text-align:left;display:inline-block;margin:10px 0px;">download-templet</a>
			  </p>
			  <div>
			<form name="import-user" action="import-user.do" method="POST" enctype="multipart/form-data">
			  <#if error?? >
				<p><span class="error"><strong>Note</strong>: ${error}!</span></p>
			  </#if>
			  <label for="user" style="color:#555">import-user:</label>
			  <input name="user" type="file" id="user" style="width:500px;display:block" />
			  <input type="submit" value="submit" /> 
			</form>
		  </div>
			</div> <#-- end of content -->
			<div class="bottom"><br /></div>
		</div> <#-- end of main -->
		<#include "footer.ftl">
	</div>

	<script src="resources/js/bootstrap.min.js"></script>
</body>
</html>