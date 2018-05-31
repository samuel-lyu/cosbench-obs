<div id="echartsDiv" style="width:1000px;height:500px"> </div>

<script type="text/javascript">
	$("#echartsDiv").css( 'width', (window.innerWidth-170)*0.95+'px');
	$("#echartsDiv").css( 'height', (window.innerHeight-200)*0.8+'px');
	var timeStampArray = new Array();
	<#list allSnapshots as ssInfo >
		timeStampArray.push('${ssInfo.timestamp?time}');
	</#list>
	
	var yAxisName = ${yAxisName};
	var allMetricsName = ${allMetricsName};
	var allMetricsData = ${allMetricsData};
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
        	data:allMetricsName,
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
	    	name: yAxisName,
	        type: 'value'
	    },
	    series:allMetricsData
    };
    myChart.setOption(option);
</script>