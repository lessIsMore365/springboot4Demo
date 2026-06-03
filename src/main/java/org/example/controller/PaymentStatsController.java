package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.PaymentStatsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment/stats")
@RequiredArgsConstructor
public class PaymentStatsController {

    private final PaymentStatsService statsService;

    // ---- REST API endpoints ----

    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String paymentMethod) {
        LocalDate[] range = parseRange(startDate, endDate);
        Map<String, Object> data = statsService.getOverview(range[0].atStartOfDay(), range[1].atTime(LocalTime.MAX), paymentMethod);
        return Map.of("success", true, "data", data, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/trend")
    public Map<String, Object> trend(@RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate,
                                     @RequestParam(required = false) String paymentMethod) {
        LocalDate[] range = parseRange(startDate, endDate);
        List<Map<String, Object>> data = statsService.getTrend(range[0].atStartOfDay(), range[1].atTime(LocalTime.MAX), paymentMethod);
        return Map.of("success", true, "data", data, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/by-method")
    public Map<String, Object> byMethod(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate) {
        LocalDate[] range = parseRange(startDate, endDate);
        List<Map<String, Object>> data = statsService.getStatsByMethod(range[0].atStartOfDay(), range[1].atTime(LocalTime.MAX));
        return Map.of("success", true, "data", data, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/by-biz-type")
    public Map<String, Object> byBizType(@RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate,
                                         @RequestParam(required = false) String paymentMethod) {
        LocalDate[] range = parseRange(startDate, endDate);
        List<Map<String, Object>> data = statsService.getStatsByBizType(range[0].atStartOfDay(), range[1].atTime(LocalTime.MAX), paymentMethod);
        return Map.of("success", true, "data", data, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/by-status")
    public Map<String, Object> byStatus(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String paymentMethod) {
        LocalDate[] range = parseRange(startDate, endDate);
        List<Map<String, Object>> data = statsService.getStatsByStatus(range[0].atStartOfDay(), range[1].atTime(LocalTime.MAX), paymentMethod);
        return Map.of("success", true, "data", data, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/recent")
    public Map<String, Object> recentOrders() {
        List<Map<String, Object>> data = statsService.getRecentOrders();
        return Map.of("success", true, "data", data, "timestamp", System.currentTimeMillis());
    }

    // ---- ECharts 可视化仪表盘 ----

    @GetMapping(value = "/chart", produces = MediaType.TEXT_HTML_VALUE)
    public String chart() {
        return CHART_HTML;
    }

    private LocalDate[] parseRange(String startDate, String endDate) {
        LocalDate end = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : end.minusDays(30);
        return new LocalDate[]{start, end};
    }

    private static final String CHART_HTML = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>支付统计仪表盘</title>
            <script src="https://cdn.jsdelivr.net/npm/echarts@5.6.0/dist/echarts.min.js"></script>
            <style>
              * { margin: 0; padding: 0; box-sizing: border-box; }
              body { background: #1a1a2e; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; color: #e0e0e0; }
              .header { padding: 16px 24px; background: #16213e; border-bottom: 1px solid #2a2a4a; display: flex; justify-content: space-between; align-items: center; }
              .header h1 { font-size: 18px; font-weight: 600; }
              .header .info { font-size: 12px; color: #888; }
              .filters { padding: 12px 24px; background: #16213e; display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
              .filters label { font-size: 12px; color: #a0a0a0; }
              .filters input, .filters select { background: #1a1a2e; border: 1px solid #3a3a5a; color: #e0e0e0; padding: 4px 8px; border-radius: 4px; font-size: 12px; }
              .filters button { background: #4fc3f7; color: #1a1a2e; border: none; padding: 5px 14px; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 600; }
              .dashboard { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; padding: 16px 24px; }
              .overview-card { background: #16213e; border-radius: 8px; padding: 16px; text-align: center; }
              .overview-card .value { font-size: 24px; font-weight: 700; margin: 4px 0; }
              .overview-card .label { font-size: 11px; color: #888; }
              .overview-card .sub { font-size: 11px; color: #4fc3f7; margin-top: 4px; }
              .charts { display: grid; grid-template-columns: 2fr 1fr; gap: 12px; padding: 0 24px 16px; }
              .charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 0 24px 16px; }
              .chart-card { background: #16213e; border-radius: 8px; padding: 16px; }
              .chart-card h3 { font-size: 13px; font-weight: 500; margin-bottom: 8px; color: #a0a0a0; }
              .chart { width: 100%; height: 320px; }
              .chart-lg { width: 100%; height: 360px; }
            </style>
            </head>
            <body>
            <div class="header">
              <h1>支付统计仪表盘</h1>
              <span class="info" id="refreshInfo">自动刷新: 5秒</span>
            </div>
            <div class="filters">
              <label>开始日期</label>
              <input type="date" id="startDate">
              <label>结束日期</label>
              <input type="date" id="endDate">
              <label>支付方式</label>
              <select id="paymentMethod">
                <option value="">全部</option>
                <option value="ALIPAY">支付宝</option>
                <option value="WECHAT">微信支付</option>
              </select>
              <button onclick="refresh()">查询</button>
            </div>
            <div class="dashboard" id="overviewCards"></div>
            <div class="charts">
              <div class="chart-card">
                <h3>收入趋势</h3>
                <div id="trendChart" class="chart-lg"></div>
              </div>
              <div class="chart-card">
                <h3>支付方式分布</h3>
                <div id="methodChart" class="chart"></div>
              </div>
            </div>
            <div class="charts-row">
              <div class="chart-card">
                <h3>业务分类分布</h3>
                <div id="bizTypeChart" class="chart"></div>
              </div>
              <div class="chart-card">
                <h3>订单状态分布</h3>
                <div id="statusChart" class="chart"></div>
              </div>
            </div>
            <script>
              const REFRESH_MS = 5000;
              const trendChart = echarts.init(document.getElementById('trendChart'));
              const methodChart = echarts.init(document.getElementById('methodChart'));
              const bizTypeChart = echarts.init(document.getElementById('bizTypeChart'));
              const statusChart = echarts.init(document.getElementById('statusChart'));

              // Token auth: URL param or localStorage
              const params = new URLSearchParams(window.location.search);
              const token = params.get('token') || localStorage.getItem('payment_stats_token') || '';
              if (token) {
                localStorage.setItem('payment_stats_token', token);
                if (params.has('token')) {
                  const url = new URL(window.location);
                  url.searchParams.delete('token');
                  window.history.replaceState({}, '', url);
                }
              }
              const AUTH_HEADER = token ? { 'Authorization': 'Bearer ' + token } : {};

              // Init date pickers: last 30 days
              const now = new Date();
              document.getElementById('endDate').value = now.toISOString().slice(0, 10);
              const start = new Date(now.getTime() - 30 * 86400000);
              document.getElementById('startDate').value = start.toISOString().slice(0, 10);

              function getFilterParams() {
                let p = '';
                const sd = document.getElementById('startDate').value;
                const ed = document.getElementById('endDate').value;
                const pm = document.getElementById('paymentMethod').value;
                if (sd) p += '&startDate=' + sd;
                if (ed) p += '&endDate=' + ed;
                if (pm) p += '&paymentMethod=' + pm;
                return p;
              }

              function formatAmount(v) {
                if (v == null) return '¥0';
                return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
              }

              async function refresh() {
                const filter = getFilterParams();
                try {
                  const [overviewRes, trendRes, methodRes, bizTypeRes, statusRes] = await Promise.all([
                    fetch('/api/payment/stats/overview?' + filter, { headers: AUTH_HEADER }),
                    fetch('/api/payment/stats/trend?' + filter, { headers: AUTH_HEADER }),
                    fetch('/api/payment/stats/by-method?' + filter, { headers: AUTH_HEADER }),
                    fetch('/api/payment/stats/by-biz-type?' + filter, { headers: AUTH_HEADER }),
                    fetch('/api/payment/stats/by-status?' + filter, { headers: AUTH_HEADER })
                  ]);
                  const overview = (await overviewRes.json()).data || {};
                  const trend = (await trendRes.json()).data || [];
                  const method = (await methodRes.json()).data || [];
                  const bizType = (await bizTypeRes.json()).data || [];
                  const status = (await statusRes.json()).data || [];

                  renderOverview(overview);
                  renderTrend(trend);
                  renderMethodPie(method);
                  renderBizTypePie(bizType);
                  renderStatusPie(status);

                  document.getElementById('refreshInfo').textContent =
                    '上次刷新: ' + new Date().toLocaleTimeString() + ' | 自动刷新: 5秒';
                } catch (e) { console.error('Refresh error:', e); }
              }

              function renderOverview(d) {
                const cards = [
                  { label: '总订单数', value: (d.totalorders || 0).toLocaleString(), color: '#4fc3f7' },
                  { label: '总金额', value: formatAmount(d.totalamount), color: '#81c784' },
                  { label: '支付成功', value: (d.successcount || 0).toLocaleString(),
                    sub: '成功率 ' + (d.totalorders > 0 ? (d.successcount / d.totalorders * 100).toFixed(1) : 0) + '%',
                    color: '#66bb6a' },
                  { label: '退款', value: (d.refundcount || 0).toLocaleString(),
                    sub: formatAmount(d.refundamount), color: '#e57373' }
                ];
                document.getElementById('overviewCards').innerHTML = cards.map(c =>
                  '<div class="overview-card">' +
                  '<div class="value" style="color:' + c.color + '">' + c.value + '</div>' +
                  '<div class="label">' + c.label + '</div>' +
                  (c.sub ? '<div class="sub">' + c.sub + '</div>' : '') +
                  '</div>'
                ).join('');
              }

              function renderTrend(data) {
                const dates = data.map(d => d.date);
                const amounts = data.map(d => Number(d.totalamount || 0));
                const counts = data.map(d => Number(d.ordercount || 0));
                trendChart.setOption({
                  tooltip: {
                    trigger: 'axis',
                    backgroundColor: 'rgba(0,0,0,.7)',
                    borderColor: '#333',
                    textStyle: { color: '#e0e0e0', fontSize: 12 }
                  },
                  legend: { data: ['金额', '订单数'], textStyle: { color: '#888', fontSize: 11 }, top: 0 },
                  grid: { left: 60, right: 30, top: 30, bottom: 30 },
                  xAxis: {
                    type: 'category', data: dates,
                    axisLine: { lineStyle: { color: '#444' } },
                    axisLabel: { color: '#888', fontSize: 10, rotate: dates.length > 20 ? 45 : 0 }
                  },
                  yAxis: [
                    {
                      type: 'value', name: '金额',
                      axisLabel: { color: '#888', fontSize: 10, formatter: v => formatAmount(v) },
                      splitLine: { lineStyle: { color: '#2a2a3a' } }
                    },
                    {
                      type: 'value', name: '订单数',
                      axisLabel: { color: '#888', fontSize: 10 },
                      splitLine: { show: false }
                    }
                  ],
                  series: [
                    {
                      name: '金额', type: 'bar', data: amounts,
                      itemStyle: { color: '#4fc3f7', borderRadius: [4, 4, 0, 0] }
                    },
                    {
                      name: '订单数', type: 'line', yAxisIndex: 1, data: counts,
                      smooth: true, symbol: 'circle', symbolSize: 4,
                      lineStyle: { color: '#ffb74d', width: 2 },
                      itemStyle: { color: '#ffb74d' }
                    }
                  ]
                }, true);
              }

              function renderMethodPie(data) {
                methodChart.setOption({
                  tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)',
                    backgroundColor: 'rgba(0,0,0,.7)', borderColor: '#333', textStyle: { color: '#e0e0e0' } },
                  series: [{
                    type: 'pie', radius: ['50%', '75%'], center: ['50%', '55%'],
                    label: { color: '#888', fontSize: 11 },
                    data: data.map(d => ({ name: d.method === 'ALIPAY' ? '支付宝' : d.method === 'WECHAT' ? '微信支付' : d.method, value: Number(d.totalamount || 0) })),
                    itemStyle: { borderRadius: 4, borderColor: '#1a1a2e', borderWidth: 2 }
                  }]
                }, true);
              }

              function renderBizTypePie(data) {
                bizTypeChart.setOption({
                  tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)',
                    backgroundColor: 'rgba(0,0,0,.7)', borderColor: '#333', textStyle: { color: '#e0e0e0' } },
                  series: [{
                    type: 'pie', radius: ['50%', '75%'], center: ['50%', '55%'],
                    label: { color: '#888', fontSize: 11 },
                    data: data.map(d => ({ name: d.biztype || '未分类', value: Number(d.totalamount || 0) })),
                    itemStyle: { borderRadius: 4, borderColor: '#1a1a2e', borderWidth: 2 }
                  }]
                }, true);
              }

              function renderStatusPie(data) {
                const colorMap = { SUCCESS: '#66bb6a', PENDING: '#ffb74d', REFUND: '#e57373', CLOSED: '#90a4ae' };
                statusChart.setOption({
                  tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)',
                    backgroundColor: 'rgba(0,0,0,.7)', borderColor: '#333', textStyle: { color: '#e0e0e0' } },
                  series: [{
                    type: 'pie', radius: ['50%', '75%'], center: ['50%', '55%'],
                    label: { color: '#888', fontSize: 11 },
                    data: data.map(d => ({
                      name: d.status, value: Number(d.ordercount || 0),
                      itemStyle: { color: colorMap[d.status] || '#78909c' }
                    })),
                    itemStyle: { borderRadius: 4, borderColor: '#1a1a2e', borderWidth: 2 }
                  }]
                }, true);
              }

              refresh();
              setInterval(refresh, REFRESH_MS);
              window.addEventListener('resize', () => {
                trendChart.resize();
                methodChart.resize();
                bizTypeChart.resize();
                statusChart.resize();
              });
            </script>
            </body>
            </html>
            """;
}
