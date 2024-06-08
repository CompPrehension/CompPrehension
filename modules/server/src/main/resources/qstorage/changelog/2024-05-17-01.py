

import json
import os
import re
import sys

question_name_suffix_regex = re.compile(r"__\d{10}_+v.*$", re.IGNORECASE)
invalid_json_paths = []

def process_json_file(filepath):
    # parse json file
    try:
        with open(filepath, encoding="utf8") as f:
            data = json.load(f)
    except Exception as e:
        print(f'Error parsing {filepath}: {e}')
        invalid_json_paths.append(filepath)
        return
    

    # extract data
    domainType = data['questionData']['questionDomainType']
    domain = 'control_flow' if domainType == 'OrderActs' else 'expression' if domainType == 'OrderOperators' else None
    if domain is None:
        print(f'Unknown domain type: {data["questionData"]["questionDomainType"]} in {filepath}')
        return
    
    # process only questions with metadata
    metadata = None
    if 'metadata' in data:
        metadata = data['metadata']
    if not metadata and 'questionData' in data and 'options' in data['questionData'] and 'metadata' in data['questionData']['options']:
        metadata = data['questionData']['options']['metadata']
    if not metadata:
        return
    
    # update templateId
    if 'templateId' in metadata:
        del metadata['templateId']
    questionName = data['questionData']['questionName']
    
    # extract templateId
    newTemplateId = question_name_suffix_regex.sub('', questionName)

    # update json
    metadata['templateId'] = newTemplateId
    if 'dateCreated' not in metadata and 'dateLastUsed' in metadata:
        metadata['dateCreated'] = metadata['dateLastUsed']
    if 'stage' in metadata:
        del metadata['stage']
    if 'usedCount' in metadata:
        del metadata['usedCount']
    if 'dateLastUsed' in metadata:
        del metadata['dateLastUsed']
    if 'lastAttemptId' in metadata:
        del metadata['lastAttemptId']
    if 'isDraft' in metadata:
        del metadata['isDraft']
    if 'qrlogIds' in metadata:
        del metadata['qrlogIds']
    if 'conceptBitsInPlan' in metadata:
        del metadata['conceptBitsInPlan']
    if 'conceptBitsInRequest' in metadata:
        del metadata['conceptBitsInRequest']
    if 'violationBitsInPlan' in metadata:
        del metadata['violationBitsInPlan']
    if 'violationBitsInRequest' in metadata:
        del metadata['violationBitsInRequest']
    # remove metadata duplicate
    if 'questionData' in data and 'options' in data['questionData'] and 'metadata' in data['questionData']['options']:
        del data['questionData']['options']['metadata']
    # update options for expression domain
    if domain == 'expression':
        newOptions = dict()
        newOptions['requireContext'] = True
        newOptions['showSupplementaryQuestions'] = True
        newOptions['showTrace'] = True
        newOptions['multipleSelectionEnabled'] = False
        newOptions['orderNumberOptions'] = dict()
        newOptions['orderNumberOptions']['delimiter'] = '#'
        newOptions['orderNumberOptions']['position'] = 'BOTTOM'
        data['questionData']['options'] = newOptions

    # save json file
    with open(filepath, 'w', encoding="utf8") as f:
        json.dump(data, f, indent=2)

if __name__ == "__main__":    
    if len(sys.argv) < 2:
        basedir = os.getcwd()
    else:
        basedir = sys.argv[1]
    print(f"Basedir: {basedir}")

    # calculate total jsons to process in basedir and subdirs
    total_jsons = 0
    for root, dirs, files in os.walk(basedir):
        for filename in files:
            if filename.endswith('.json'):
                total_jsons += 1
    print(f"Total jsons to process: {total_jsons}")

    processed_jsons = 0
    for root, dirs, files in os.walk(basedir):
        for filename in files:
            if filename.endswith('.json'):
                print(f"Processing: {os.path.join(root, filename)}")
                filepath = os.path.join(root, filename)
                process_json_file(filepath)
                processed_jsons += 1
                print(f"Processed json: {processed_jsons}/{total_jsons} ({processed_jsons/total_jsons:.2%})")

    if len(invalid_json_paths) > 0:
        print(f"Total invalid json paths: {len(invalid_json_paths)}")
        print(f"Invalid json paths:")
        for path in invalid_json_paths:
            print(path)

        print("Remove them? (y/n)")
        answer = input()
        if answer == 'y':
            for path in invalid_json_paths:
                os.remove(path)
                print(f"Removed: {path}")

    print("Done")


