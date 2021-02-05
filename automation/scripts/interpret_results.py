import sys, os
import xmltodict, json
import pprint
from datetime import datetime

"""
This script parses gatling automated test results into a format expected by 
https://github.com/broadinstitute/firecloud-automated-testing. Gatling does not
print the "type" attribute that is desired on the failures, and therefore the failures do
not get picked up and sent to BQ.

This script also serves as a way of parsing the results to measure the performance
over time of these tests.

Auther: @rfricke-asymmetrik
"""

pp = pprint.PrettyPrinter()

def get_assertions_xml(test_dir, simulation):
    sim_dir = os.path.join(test_dir, "test-reports", "TEST-" + simulation + ".xml")
    return xmltodict.parse(open(sim_dir, 'rb'), force_list={'testcase'})

def get_test_case(line, className):
    message = line[line.find('KO\t') + 3:].replace('\n', '')
    text = message
    sim_split = line.split('\t')
    start = int(sim_split[3])
    end = int(sim_split[4])
    time = end - start
    name = className.split('.')[len(className.split('.')) - 1]

    return {
        '@class': className,
        '@name': name,
        '@time': time,
        'failure': {
            '@message': message,
            '@type': 'class ' + className,
            '#text': text
        }
    }

def get_failures(lines, className):
    failures = filter(lambda l: l.find("\tKO\t") > -1, lines)
    return list(map(lambda k: get_test_case(k, className), failures))

def update_xml(assertions_xml, failures):
    test_suite = assertions_xml['testsuite']
    if len(failures) > 0:
        test_suite.pop('testcase', None)
        test_suite['testcase'] = failures
        test_suite['@failures'] = len(failures)
        test_suite['@tests'] = len(failures)
        assertions_xml['testsuite'] = test_suite
    return assertions_xml

def strip_chars(str):
    return str.replace('\n', '')

def toBQTimestamp(ts):
    time_format = '%Y-%m-%d %H:%M:%S.%f'
    return datetime.utcfromtimestamp(ts / 1000.0).strftime(time_format)[:-3] + " UTC"

def build_scenario_performance(simulation_id, simulation_lines):
    scenario_performance_array = []
    request_array = []
    scenario = {}
    start = 0

    for line in simulation_lines:
        line_split = line.split('\t')
        
        if line_split[0] == 'USER':
            if line_split[2] == 'START':
                start = float(strip_chars(line_split[3]))
                scenario = {
                    'scenario_id': line_split[1] + '|' + strip_chars(line_split[3]),
                    'scenario': line_split[1],
                    'start': toBQTimestamp(start),
                    'simulation_id': simulation_id
                }
            elif line_split[2] == 'END':
                scenario['end'] = toBQTimestamp(float(strip_chars(line_split[3])))
                scenario['time'] = int(float(strip_chars(line_split[3])) - start)

                scenario_performance_array.append(scenario)
        elif line_split[0] == 'REQUEST':
            request_array.append({
                'request_id': line_split[2] + '|' + strip_chars(line_split[3]),
                'request': line_split[2],
                'scenario_id': scenario['scenario_id'],
                'start': toBQTimestamp(float(strip_chars(line_split[3]))),
                'end': toBQTimestamp(float(strip_chars(line_split[4]))),
                'time': int(strip_chars(line_split[4])) - int(strip_chars(line_split[3])),
                'result': line_split[5]
            })

    return {
        'scenarios': scenario_performance_array,
        'requests': request_array
    }

def build_json(assertions_json, simulation_lines):
    end = float(simulation_lines[len(simulation_lines) - 2].split('\t')[4])
    end_time = toBQTimestamp(end)
    start_time = assertions_json['start']
    simulation = {
        'simulation_id': assertions_json['simulationId'] + '|' + str(assertions_json['start']),
        'simulation': assertions_json['simulationId'],
        'start': toBQTimestamp(start_time),
        'end': end_time,
        'time': int(end - assertions_json['start'])
    }
    scenariosAndRequests = build_scenario_performance(simulation['simulation_id'], simulation_lines)

    return {
        'simulation': simulation, 
        'scenarios': scenariosAndRequests['scenarios'], 
        'requests': scenariosAndRequests['requests']
    }

def parse_gatling_results(gatling_dir):
    performance_data = {
        'simulations_xml': [],
        'simulations_json': [],
        'scenarios': [],
        'requests': []
    }
    for file_path in os.listdir(os.path.join(gatling_dir, "gatling")):
        scenario_path = os.path.join(gatling_dir, "gatling", file_path)
        simulation_path = os.path.join(scenario_path, 'simulation.log')
        js_path = os.path.join(scenario_path, 'js')

        assertion_json = json.load(open(os.path.join(js_path, 'assertions.json'), 'r'))
        simulation_log_lines = open(simulation_path, 'r').readlines()

        simulation = assertion_json['simulation']

        assertions_xml = update_xml(get_assertions_xml(gatling_dir, simulation), get_failures(simulation_log_lines, simulation))

        performance_json = build_json(assertion_json, simulation_log_lines)

        performance_data['simulations_xml'].append({
            'simulation': simulation,
            'xml': xmltodict.unparse(assertions_xml, pretty=True)
        })

        performance_data['simulations_json'].append(performance_json['simulation'])
        performance_data['scenarios'] = performance_data['scenarios'] + performance_json['scenarios']
        performance_data['requests'] = performance_data['requests'] + performance_json['requests']

    return performance_data

if __name__ == '__main__':
    r = parse_gatling_results("target")

    timestamp = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")

    for scn in r['simulations_xml']:
        sim_dir = os.path.join("target/test-reports", "TEST-" + scn['simulation'] + ".xml")
        with open(sim_dir, 'w') as wf:
            wf.write(scn['xml'])
            wf.write('\n')
        
        print(sim_dir)
    
    with open("target/test-reports/" + timestamp + "-simulation.json", 'w') as swf:
        for sim in r['simulations_json']:
            swf.write(json.dumps(sim))
            swf.write('\n')

        print("target/test-reports/" + timestamp + "-simulation.json")
    
    with open("target/test-reports/" + timestamp + "-scenario.json", 'w') as swf:
        for sc in r['scenarios']:
            swf.write(json.dumps(sc))
            swf.write('\n')

        print("target/test-reports/" + timestamp + "-scenario.json")
    
    with open("target/test-reports/" + timestamp + "-request.json", 'w') as swf:
        for rq in r['requests']:
            swf.write(json.dumps(rq))
            swf.write('\n')

        print("target/test-reports/" + timestamp + "-request.json")
        