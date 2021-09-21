from cfn_tools import load_yaml
import os

with open("quickstart/cfn-provider-registration.yaml", "r") as stream:
    tmpl = load_yaml(stream)

print(tmpl['Parameters']['Version']['Default'])
