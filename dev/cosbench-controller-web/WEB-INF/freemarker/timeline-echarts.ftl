<div id="echartsDiv" style="width:100%;height:500px"> </div>

<script type="text/javascript">
	$("#echartsDiv").css( 'width', window.innerWidth-170+'px');
	var timeStampArray = new Array();
	var readThroughputArray = new Array();
	var writeThroughputArray = new Array();
	<#list allSnapshots as ssInfo >
		timeStampArray.push('${ssInfo.timestamp?time}');
		<#assign allMetrics = ssInfo.report.allMetrics >
		<#list allMetrics as mInfo >
			<#if mInfo.opName == 'read' >
           		readThroughputArray.push(${mInfo.throughput})
         	</#if>
			<#if mInfo.opName == 'write' >
           		writeThroughputArray.push(${mInfo.throughput})
         	</#if>
		</#list>
	</#list>
	
    var myChart = echarts.init(document.getElementById('echartsDiv'));
    var option = {
        title: {
            text: 'Timeline Status'
        },
        tooltip: {
        	trigger: 'axis'
    	},
        legend: {
        data:['read','write']
    	},
    	toolbox: {
	        feature: {
	            saveAsImage: {}
	        }
    	},
        xAxis: {
	        type: 'category',
	        data: timeStampArray
	    },
	    yAxis: {
	        type: 'value'
	    },
	    series: [
		    {
		    	name:"read",
		        data: readThroughputArray,
		        type: 'line'
		    },
		    {
		    	name:"write",
		        data: writeThroughputArray,
		        type: 'line'
		    }
	    ]	
    };
    myChart.setOption(option);
</script>