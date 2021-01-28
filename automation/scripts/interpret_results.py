import sys, os
import xmltodict, json
import pprint
import datetime

"""
This script parses gatling automated test results into a format expected by 
https://github.com/broadinstitute/firecloud-automated-testing. Gatling does not
print the "type" attribute that is desired on the failures, and therefore the failures do
not get picked up and sent to BQ.

TODO: This script will also serve as a way of parsing the results to measure the performance
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
    start = float(sim_split[3])
    end = float(sim_split[4])
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

def build_scenario_performance(simulation_lines):
    scenario_performance_array = []
    request_array = []
    scenario_performance = {}

    for line in simulation_lines:
        line_split = line.split('\t')
        
        if line_split[0] == 'USER':
            if line_split[2] == 'START':
                request_array = []
                scenario_performance['start'] = float(strip_chars(line_split[3]))
            elif line_split[2] == 'END':
                scenario_performance['end'] = float(strip_chars(line_split[3]))
                scenario_performance['requests'] = request_array
                scenario_performance['time'] = scenario_performance['end'] - scenario_performance['start']

                scenario_matches = filter(lambda sp: sp['name'] == line_split[1],scenario_performance_array)
                if len(scenario_matches) == 1:
                    scn = scenario_matches[0]
                    scn['users'].append(scenario_performance)
                    scn['max'] = scn['max'] if scn['max'] >= scenario_performance['time'] else scenario_performance['time']
                    scn['total'] += scenario_performance['time']
                    scn['avg'] = scn['total'] / len(scn['users'])
                    scn['min'] = scn['min'] if scn['min'] < scenario_performance['time'] else scenario_performance['time']
                else:
                    scenario_performance_array.append({
                        'name': line_split[1],
                        'max': scenario_performance['time'],
                        'avg': scenario_performance['time'],
                        'min': scenario_performance['time'],
                        'total': scenario_performance['time'],
                        'users': [scenario_performance]
                    })
        elif line_split[0] == 'REQUEST':
            request_array.append({
                'name': line_split[2],
                'start': float(strip_chars(line_split[3])),
                'end': float(strip_chars(line_split[4])),
                'time': float(strip_chars(line_split[4])) - float(strip_chars(line_split[3])),
                'result': line_split[5]
            })

    return scenario_performance_array

def build_json(assertions_json, simulation_lines):
    assertions_json['scenarios'] = build_scenario_performance(simulation_lines)
    end_time = float(simulation_lines[len(simulation_lines) - 2].split('\t')[4])
    assertions_json['end'] = end_time
    assertions_json['time'] = end_time - assertions_json['start']
    assertions_json.pop('assertions', None)

    return assertions_json

def parse_gatling_results(gatling_dir):
    scenarios = {}
    for file_path in os.listdir(os.path.join(gatling_dir, "gatling")):
        scenario_path = os.path.join(gatling_dir, "gatling", file_path)
        simulation_path = os.path.join(scenario_path, 'simulation.log')
        js_path = os.path.join(scenario_path, 'js')

        assertion_json = json.load(open(os.path.join(js_path, 'assertions.json'), 'r'))
        simulation_log_lines = open(simulation_path, 'r').readlines()

        simulation = assertion_json['simulation']

        assertions_xml = update_xml(get_assertions_xml(gatling_dir, simulation), get_failures(simulation_log_lines, simulation))

        performance_json = build_json(assertion_json, simulation_log_lines)

        scenarios[simulation] = {
            'xml': xmltodict.unparse(assertions_xml, pretty=True),
            'json': json.dumps(performance_json, indent=4)
        }
    return scenarios

if __name__ == '__main__':
    r = parse_gatling_results("target")
    for scn in r:
        sim_dir = os.path.join("target/test-reports", "TEST-" + scn + ".xml")
        with open(sim_dir, 'w') as wf:
            wf.write(r[scn]['xml'])
            wf.write('\n')
        
        js_dir = os.path.join("target/test-reports", "TEST-" + scn + "-performance.json")
        with open(js_dir, 'w') as jwf:
            jwf.write(r[scn]['json'])
            jwf.write('\n')
        
        print(sim_dir)
        print(js_dir)
        