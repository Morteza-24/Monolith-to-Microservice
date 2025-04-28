from subprocess import run
from os import pathsep
import networkx as nx
import itertools
import json
import copy


PROJECT = "DayTrader"
ALL_MICROSERVICES = [  # you can get these from ../TopMicroservices/
['Log', 'PingBean', 'PingServlet2IncludeRcv', 'TradeConfigJSF'],
['TradeActionProducer', 'PingBean', 'DTStreamer3MDB', 'TimerStat', 'PingServlet2PDF', 'PingServletLargeContentLength'],
['HoldingData', 'TradeAppJSF', 'Log', 'TradeDirect', 'TradeConfigJSF'],
['TradeAppJSF', 'TradeScenarioServlet', 'Log', 'TradeServletAction', 'TradeSLSBLocal', 'QuoteDataBean', 'TradeConfigJSF', 'TradeAppServlet'],
['PingCDIBean', 'Log', 'ActionDecoder', 'KeySequenceDirect', 'PingServletCDIBeanManagerViaCDICurrent', 'PingServlet2JNDI', 'PingJDBCRead'],
['HoldingData', 'TradeAppJSF', 'Log', 'TradeServletAction', 'TradeDirect', 'DTStreamer3MDB', 'TradeServices', 'TimerStat', 'TradeConfigJSF', 'TradeAction', 'MDBStats', 'DTBroker3MDB', 'TradeConfig', 'TradeAppServlet', 'CompleteOrderThread', 'OrderDataJSF'],
['Log', 'PingBean', 'TradeDirect', 'TradeWebContextListener', 'TradeSLSBLocal', 'TradeConfigJSF', 'PingServletLargeContentLength', 'TradeConfig', 'PingJDBCRead', 'OrderDataJSF'],
['HoldingData', 'Log', 'ExternalContextProducer'],
['AccountProfileDataBean', 'Log', 'TradeDirect', 'TradeServices', 'MarketSummaryJSF', 'HoldingDataBean', 'FinancialUtils', 'OrderData'],
['AccountProfileDataBean', 'DTStreamer3MDB', 'ActionDecoder', 'TimerStat', 'TradeConfigJSF'],
['TradeAppJSF', 'AccountProfileDataBean', 'MarketSummaryDataBean', 'Log', 'DTStreamer3MDB', 'TradeWebContextListener', 'ActionDecoder', 'TradeSLSBBean', 'QuoteDataBean', 'MarketSummarySingleton', 'ActionMessage'],
['PingServletWriter', 'Log', 'ActionDecoder'],
['HoldingData', 'AccountProfileDataBean', 'AccountDataBean', 'MarketSummaryDataBean', 'Log', 'TradeDirect', 'TradeSLSBLocal', 'MarketSummaryJSF', 'QuoteDataBean', 'FinancialUtils'],
['AccountProfileDataBean', 'TradeWebContextListener', 'TradeConfigJSF'],
['TradeDirect', 'TradeServices', 'PingServletCDI', 'PingEJBLocalDecorator'],
['HoldingData', 'TradeSLSBLocal'],
['OrderDataBean', 'TradeAppJSF', 'JsonEncoder', 'AccountProfileDataBean', 'PingWebSocketBinary', 'OrdersAlertFilter', 'JsonMessage', 'JsonDecoder', 'JSFLoginFilter'],
['HoldingData', 'Log', 'TradeServletAction', 'TradeServices', 'ExternalContextProducer', 'QuoteDataBean'],
['Log', 'TradeWebContextListener', 'QuoteDataBean', 'TradeConfigJSF', 'Handler', 'PingJDBCRead'],
['HoldingData', 'TradeScenarioServlet', 'OrdersAlertFilter', 'TradeSLSBLocal', 'TradeAppServlet'],
['HoldingData', 'TradeAppJSF', 'PingCDIBean', 'PingSession3Object', 'AccountProfileDataBean', 'TradeScenarioServlet', 'AccountDataBean', 'MarketSummaryDataBean', 'Log', 'TradeServletAction', 'QuoteJSF', 'TradeWebContextListener', 'TradeSLSBBean', 'OrdersAlertFilter', 'AccountDataJSF', 'MarketSummaryJSF', 'QuoteDataBean', 'MarketSummarySingleton', 'KeyBlock', 'TradeAction', 'ActionMessage', 'TradeConfig', 'PortfolioJSF', 'TradeAppServlet', 'HoldingDataBean', 'FinancialUtils', 'OrderData', 'OrderDataJSF'],
['Log', 'ActionDecoder', 'TradeSLSBLocal', 'ActionMessage', 'TradeConfig', 'TradeAppServlet'],
['PingSession3Object', 'Log', 'ActionDecoder', 'TradeSLSBBean', 'QuoteDataBean', 'MarketSummarySingleton', 'TradeConfigJSF', 'OrderDataJSF'],
['TradeActionProducer', 'PingBean', 'TradeSLSBLocal', 'TradeConfigJSF'],
['JsonEncoder', 'TradeActionProducer', 'ActionDecoder', 'JsonMessage', 'QuoteDataBean', 'PingServlet2PDF', 'PingWebSocketTextSync', 'JsonDecoder'],
['HoldingData', 'TradeAppJSF', 'AccountProfileDataBean', 'ActionDecoder'],
['PingCDIBean', 'Log', 'PingBean', 'ActionDecoder', 'QuoteDataBean', 'MarketSummarySingleton', 'TradeConfigJSF'],
['HoldingData', 'PingServletWriter', 'PingCDIBean', 'PingServlet2MDBQueue', 'PingServlet2MDBTopic', 'PingSession2', 'PingSession1', 'Log', 'PingBean', 'PingServlet2Jsp', 'PingServlet', 'PingServlet31AsyncRead', 'PingServlet2DB', 'PingManagedExecutor', 'ExplicitGC', 'TestServlet', 'PingServlet2Include', 'PingServletCDI', 'PingServletSetContentLength', 'PingServlet30Async', 'PingServlet2SessionLocal', 'PingServlet2SessionRemote', 'PingServlet2Servlet', 'MarketSummaryWebSocket', 'PingServlet2PDF', 'PingServletCDIBeanManagerViaJNDI', 'PingWebSocketTextSync', 'PingReentryServlet', 'PingWebSocketJson', 'PingJSONP', 'LoginValidator', 'JSFLoginFilter', 'Listener', 'PingManagedThread', 'PingServlet2ServletRcv', 'KeySequenceDirect', 'PingServletCDIBeanManagerViaCDICurrent', 'PingEJBLocalDecorator', 'PingUpgradeServlet', 'PingServlet2JNDI', 'PingJDBCRead', 'PingWebSocketTextAsync', 'RunStatsDataBean', 'RecentStockChangeList'],
['HoldingData', 'AccountProfileDataBean', 'QuoteJSF', 'ActionDecoder', 'TradeSLSBBean', 'PingServlet2DB', 'ExplicitGC', 'MarketSummaryWebSocket', 'PingWebSocketTextSync', 'PingJSONP', 'RunStatsDataBean', 'RecentStockChangeList'],
['HoldingData', 'Log', 'MarketSummaryJSF', 'TradeConfigJSF'],
['OrderDataBean', 'HoldingData', 'PingServlet2MDBQueue', 'JsonEncoder', 'PingServlet2MDBTopic', 'TradeBuildDB', 'PingSession3', 'TradeScenarioServlet', 'PingServlet2Session2Entity2JSP', 'QuoteData', 'MarketSummaryDataBean', 'Log', 'TradeServletAction', 'QuoteJSF', 'TradeDirect', 'TradeWebContextListener', 'TradeConfigServlet', 'TradeSLSBBean', 'PingServlet2Entity', 'PingWebSocketBinary', 'PingServlet2DB', 'OrdersAlertFilter', 'PingJDBCRead2JSP', 'PingServlet2Session2EntityCollection', 'TradeSLSBLocal', 'AccountDataJSF', 'TestServlet', 'MarketSummaryJSF', 'PingServlet2Include', 'MarketSummarySingleton', 'PingServlet2SessionLocal', 'PingServlet2SessionRemote', 'TradeConfigJSF', 'PingServlet2Session2CMROne2Many', 'KeyBlock', 'MarketSummaryWebSocket', 'TradeAction', 'ActionMessage', 'PingServlet2Session2Entity', 'PingServlet31Async', 'PingJDBCWrite', 'DTBroker3MDB', 'TradeConfig', 'PingWebSocketJson', 'PingJSONP', 'PortfolioJSF', 'Listener', 'PingServlet2Session2CMROne2One', 'PingServlet2TwoPhase', 'TradeAppServlet', 'HoldingDataBean', 'PingUpgradeServlet', 'PingServlet2JNDI', 'PingJDBCRead', 'FinancialUtils', 'RunStatsDataBean', 'OrderData', 'RecentStockChangeList', 'CompleteOrderThread', 'OrderDataJSF'],
['HoldingData', 'Log', 'QuoteDataBean', 'TradeConfigJSF'],
['Log', 'MarketSummaryJSF', 'QuoteDataBean', 'TradeConfigJSF'],
['HoldingData', 'PingServletWriter', 'PingSession2', 'Log', 'PingJDBCRead'],
['Log', 'TradeWebContextListener', 'ActionMessage'],
['PingCDIBean', 'Log', 'PingBean', 'TradeWebContextListener', 'TradeSLSBLocal', 'QuoteDataBean', 'PingServletCDIBeanManagerViaJNDI', 'PingJDBCRead'],
['PingServletWriter', 'AccountProfileDataBean', 'TradeServices', 'PingServlet2DB', 'PingManagedExecutor', 'TradeConfigJSF', 'PingServletLargeContentLength', 'PingWebSocketJson'],
['PingSession3Object', 'PingInterceptor', 'TradeSLSBRemote', 'TradeConfigJSF', 'Handler', 'OrderDataJSF'],
['Log', 'TradeWebContextListener', 'ActionDecoder', 'PingJDBCRead'],
]
KEEPS = []


libs = "../../Mo2oM/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar"+pathsep+"../../Mo2oM/JavaParser/lib/json-20230618.jar"
json_path = "../../Mo2oM/JavaParser/classes.json"
run(['java', '-cp', libs, "../../Mo2oM/JavaParser/Parser.java", f"../../TestProjects/{PROJECT}/OneFileSource.java", json_path])
with open(json_path, "rt") as classes_file:
	classes_info = json.load(classes_file)


def calc_inter_intra(MICROSERVICES):
	classes_info_cpy = copy.deepcopy(classes_info)
	ms_ranges = []
	len_ranges = 0
	for ms in MICROSERVICES:
		ms_ranges.append((len_ranges, len_ranges + len(ms)))
		len_ranges += len(ms)

	nodes_labels = []
	for ms in MICROSERVICES:
		nodes_labels.extend(ms)


	immutables = []
	current_ms = 0
	for i, clss in enumerate(nodes_labels):
		if i == ms_ranges[current_ms][1]:
			current_ms += 1
		for j, call in enumerate(classes_info_cpy[clss]["method_calls"]):
			if (clss, j) in immutables:
				continue
			for other_clss in nodes_labels:
				if call["method_name"] in classes_info_cpy[other_clss]["methods"]:
					call["class_name"] = other_clss
					if clss == other_clss:
						break
					if call["class_name"] in nodes_labels[ms_ranges[current_ms][0]:ms_ranges[current_ms][1]]:
						immutables.append((clss, j))
						break

	inter = 0
	intra = 0
	all_edges = []
	current_ms = 0
	for i, clss in enumerate(nodes_labels):
		if i == ms_ranges[current_ms][1]:
			current_ms += 1
		for call in classes_info_cpy[clss]["method_calls"]:
			if call["class_name"] in nodes_labels:
				if call["class_name"] == clss:
					continue
				if call["class_name"] in MICROSERVICES[current_ms]:
					idx = ms_ranges[current_ms][0] + nodes_labels[ms_ranges[current_ms][0]:ms_ranges[current_ms][1]].index(call["class_name"])
					inter += 1
					all_edges.append((i, idx))
				else:
					all_edges.append((i, nodes_labels.index(call["class_name"])))
					intra += 1
	edges = [edge for edge in all_edges if all_edges.count(edge) > 1]

	labels = {}
	current_ms = 0
	for i in range(len(nodes_labels)):
		if i == ms_ranges[current_ms][1]:
			current_ms += 1
		labels[i] = current_ms

	G = nx.Graph()
	G.add_nodes_from([(i, {"label": lbl}) for i, lbl in enumerate(nodes_labels)])
	G.add_edges_from(edges)

	inter = sum([1 if labels[edge[0]] == labels[edge[1]] else 0 for edge in G.edges()])
	intra = len(G.edges()) - inter
	return inter, intra


for ams in ALL_MICROSERVICES:
	MICROSERVICES = [ams]
	if calc_inter_intra(MICROSERVICES)[0] >= len(ams):
		KEEPS.append(ams)

print()
for k in KEEPS:
	print(k)
print()

min_intra = float("inf")
min_comb = None
for comb in itertools.combinations(KEEPS, 4):
	this_intra = calc_inter_intra(list(comb))[1]
	if this_intra < min_intra:
		min_intra = this_intra
		min_comb = comb
		print(min_intra)
	if this_intra <= 2:
		print()
		print(f"Found a combination with intra = {this_intra}")
		print(comb)
		print()

print()
print(f"Minimum intra: {min_intra}")
print(min_comb)
