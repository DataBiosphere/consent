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

def parse_gatling_results(gatling_dir):
    scenarios = {}
    for file_path in os.listdir(os.path.join(gatling_dir, "gatling")):
        scenario_path = os.path.join(gatling_dir, "gatling", file_path)
        simulation_path = os.path.join(scenario_path, 'simulation.log')
        js_path = os.path.join(scenario_path, 'js')

        assertion_json = json.load(open(os.path.join(js_path, 'assertions.json'), 'r'))
        simulation_log_lines = open(simulation_path, 'r').readlines()

        simulation = assertion_json['simulation']

        failures = get_failures(simulation_log_lines, simulation)

        assertions_xml = get_assertions_xml(gatling_dir, simulation)
        test_suite = assertions_xml['testsuite']
        if len(failures) > 0:
            test_suite.pop('testcase', None)
            test_suite['testcase'] = failures
            test_suite['@failures'] = len(failures)
            test_suite['@tests'] = len(failures)
            assertions_xml['testsuite'] = test_suite

        scenarios[simulation] = xmltodict.unparse(assertions_xml, pretty=True)
    return scenarios

if __name__ == '__main__':
    r = parse_gatling_results("target")
    for scn in r:
        sim_dir = os.path.join("target/test-reports", "TEST-" + scn + ".xml")
        with open(sim_dir, 'w') as wf:
            wf.write(r[scn])
            wf.write('\n')
        
        print(sim_dir)
        