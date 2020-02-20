#!/usr/bin/env python

"""Created by gudrun 15/09/17
This script creates a html test summary report for all GOR subproject
The script can be run manually on a command line or via Gradle Task: generateTestReport
"""


import sys, os, webbrowser

try:
    import numpy
except ImportError:
    sys.exit("You need to import numpy to run this script.")

try:
    from bs4 import BeautifulSoup
except ImportError:
    sys.exit("You need to import BeautifulSoup to run this script.")


"""
Parses nested values from unit test html reports
:param path: path to index.html report file
:param id: html tag id
:returns: Text value in nested tag
"""
def getHtmlTagById(path, id):
    html_doc = open(path, 'r')
    soup = BeautifulSoup(html_doc, 'html.parser')
    tag = soup.find('div', attrs={'id': id})
    value = '-'
    if tag:
        value = tag.find('div').text
    return value

"""
:param ttime: formatted time string ex. 1m2.345s
:returns: Time as a float tuple (min,sec)
"""
def parseTime(ttime):
    min = 0.0
    sec = 0.0
    if 'h' in ttime:
        hr = float(ttime.split('h')[0])
        min += hr * 60.0
        ttime = ttime.split('h')[1]
    if 'm' in ttime:
        min += float(ttime.split('m')[0])
        ttime = ttime.split('m')[1]
    if 's' in ttime:
        sec = ttime.strip('s')
    return min,float(sec)


"""
:param ttime: time as float tuple (min,sec)
:returns: Time as a formatted string: 1m 2.345s
"""
def getTimeStr(ttime):
    str_time = ttime
    if ttime != "-":
        min = int(ttime[0])
        sec = ttime[1]
        min += int(sec) / 60
        sec = sec % 60
        str_time = str(min) + 'm ' + "%.3f" % sec + 's'

        if min == 0:
            str_time = str_time.split('m ')[1]
    return str_time


"""
:param ttime: time as float tuple (min,sec)
:param count: number of tests performed
:returns: Average time per test in seconds
"""
def calcAvg(ttime,count):
    avg = 0
    if int(count) > 0:
        min = ttime[0]
        sec = ttime[1]
        total_sec = (min * 60.0) + sec
        avg = total_sec / float(count)
    return avg


"""
:param path: index.html file location
:returns: project test data parsed from file
"""
def parseHtmlData(path):
    ttime = getHtmlTagById(path,'duration')
    perc = getHtmlTagById(path,'successRate')
    count = getHtmlTagById(path,'tests')
    avg = calcAvg(parseTime(ttime),count)
    return [ttime,perc,count,avg]


"""
Collects available tests data from all sub-projects in GOR 
:returns: a dictionary with project category as a key
"""
def collectTestData():
    tests = dict()
    tests['Unit'] = set()
    tests['Slow'] = set()
    tests['Integration'] = set()

    #walk through all sub directories
    for root,dirs,files in os.walk('.'):
        project = root[2:].split('/')[0]
        path = root[2:] + "/index.html"
        if ("/build/reports/tests" in root and "index.html" in files ):
            pd = parseHtmlData(path)
            tests['Unit'].add((project,path,pd[0],pd[1],pd[2],pd[3]))
        if ("/build/reports/slowTests" in root and "index.html" in files ):
            pd = parseHtmlData(path)
            tests['Slow'].add((project,path,pd[0],pd[1],pd[2],pd[3]))
        if ("/build/reports/integrationTests" in root and "index.html" in files ):
            pd = parseHtmlData(path)
            tests['Integration'].add((project,path,pd[0],pd[1],pd[2],pd[3]))

    return tests


"""
:param tests: test results stored as a collection (set)
:param key: unit test category [Unit,Slow,Integration]
:returns: html representation of the collected test data
"""
def createTestBody(tests,key):
    html  = """<details><summary><span>""" + key + """ Tests !time!</span><span>!avg! avg</span></summary><table>
                <tr class="top_row"><td>project</td><td>time</td><td>%</td><td>tests</td><td>avg</td></tr>"""

    total_time = (0.0 , 0.0)
    total_tests = 0
    sorted_tests  = sorted(tests,key=lambda x: x[5]) #sort based on avg value

    for p in sorted_tests:
        html += """<tr><td><a href='""" + p[1] + """'>""" + p[0] + """</a></td>"""
        html += """<td>""" + p[2] + """</td><td>""" + p[3] + """</td><td>""" + p[4] + """</td><td>""" + getTimeStr((0,p[5])) + """</td></tr>"""
        total_time = tuple(numpy.add(total_time , parseTime(p[2])))
        total_tests += int(p[4])

    total_avg = calcAvg(total_time,total_tests)

    html = html.replace('0.000s','-')
    html = html.replace('!time!', getTimeStr(total_time))
    html = html.replace('!avg!',  getTimeStr((0,total_avg)))
    html += """</table></details><br><br>"""

    return html

"""
:returns: a html test report with links to all sub-tests
"""
def createReport():
    msg_head  = """<html><head><title>GOR Tests Report</title></head><style>td{width:100px} span{margin-right:20px} .top_row{font-weight:bold}</style><body>"""
    msg_tail  = """</body></html>"""
    msg_body  = ""

    test_data = collectTestData()

    for key in test_data:
        msg_body += createTestBody(test_data[key],key)

    html = msg_head + msg_body + msg_tail
    return html


html = createReport()
soup = BeautifulSoup(html, 'html.parser')
html = soup.prettify().encode('utf-8')

filename = 'report.html'
f = open(filename,'w')
f.write(html)
f.close()

fullpath = 'file://' + os.getcwd() + '/' + filename
webbrowser.open_new_tab(fullpath)
