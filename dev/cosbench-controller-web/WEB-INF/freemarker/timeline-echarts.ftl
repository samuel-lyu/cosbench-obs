<div id="echartsDiv" style="width:1000px;height:500px"> </div>

<script type="text/javascript">
	$("#echartsDiv").css( 'width', (window.innerWidth-170)*0.95+'px');
	$("#echartsDiv").css( 'height', (window.innerHeight-200)*0.8+'px');
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
            text: 'Timeline Status',
            x: 'left'
        },
        tooltip: {
        	trigger: 'axis'
    	},
        legend: {
        	data:['read','write'],
        	x: 'center'
    	},
    	grid: {
	        left: 60,
	    },
    	toolbox: {
	        feature: {
	            saveAsImage: {}
	        }
    	},
        xAxis: {
        	name: 'TimeStamp',
	        type: 'category',
	        data: timeStampArray
	    },
	    yAxis: {
	    	name: 'Throughput(op/s)',
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