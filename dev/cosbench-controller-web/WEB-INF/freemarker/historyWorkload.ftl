<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta http-equiv="refresh" content="60">
	<link href="resources/css/bootstrap.min.css" rel='stylesheet' type='text/css' />
	<link href="resources/css/style.css" rel='stylesheet' type='text/css' />
	<link href="resources/css/font-awesome.css" rel="stylesheet"> 
	<link href='https://fonts.googleapis.com/css?family=Roboto:700,500,300,100italic,100,400' rel='stylesheet' type='text/css'>
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
	function resubmitWorkloads() {
		var hids = findChecked('HistoryWorkload');
		var aids = findChecked('ArchivedWorkload');
		var ids = '';
		if(hids.length > 0)
			ids = hids + '_';
		ids += aids;
		document.getElementById('resubmitIds').value = ids;
		document.getElementById('resubmitForm').submit();
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
			  
			  <div>
				<h3>Archived Workloads  <span class="counter state">${archInfos?size}</span></h3>
				<p><a href="matrix.html?type=arch&ops=read&ops=write&ops=delete&metrics=rt&rthisto=_95rt&metrics=t&metrics=succ">view performance matrix</a></p>
				<p>
					<#if loadArch == false>
					  <p><a href="historyWorkload.html?loadArch=true">load archived workloads</a></p>
					  <#elseif loadArch == true>
					  <p><a href="historyWorkload.html?loadArch=false" style="display:none">unload archived workloads</a></p>
					</#if>
				</p>
				
				<#if loadArch == true>
				<table class="info-table">
				  <tr>
					<th style="width:5%;"><input type="checkbox" id="AllArchived" onclick="checkAll(event,'ArchivedWorkload')"></th>
					<th class="id" style="width:5%;">ID</th>
					<th>Name</th>
					<th>Duration</th>
					<th>Op-Info</th>
					<th>State</th>
					<th style="width:15%;">Link</th>
				  </tr>
				   <#list archInfos as aInfo >
					<tr>
					  <td><input type="checkbox" id="checkbox-${aInfo.id}" name="ArchivedWorkload" onclick="checkItem(event,'AllArchived')" value="${aInfo.id}"></td>
					  <td onclick="checkMe('${aInfo.id}','AllArchived');");">${aInfo.id}</td>
					  <td onclick="checkMe('${aInfo.id}','AllArchived');");">${aInfo.workload.name}</td>
					  <td onclick="checkMe('${aInfo.id}','AllArchived');");"><#if aInfo.startDate?? >${aInfo.startDate?datetime}<#else>N/A</#if> - ${aInfo.stopDate?time}</td>
					  <td onclick="checkMe('${aInfo.id}','AllArchived');");">
						<#list aInfo.allOperations as op >
						  ${op}<#if op_has_next>,</#if>
						</#list>
					  </td>
					  <td onclick="checkMe('${aInfo.id}','AllArchived');"><span class="workload-state-${aInfo.state?lower_case} state">${aInfo.state?lower_case}</span></td>
					  <td><a href="workload.html?id=${aInfo.id}">view details</a></td>
					</tr>
				  </#list>
				</table>
				</#if>
				
				<form id="resubmitForm" method="POST" action="historyWorkload.html">
					<input id="resubmitIds" type="hidden" name="resubmitIds" value="">
					<input type="hidden" name="resubmit" value="yes">
					<input type="button" onclick="resubmitWorkloads();" value="resubmit">
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