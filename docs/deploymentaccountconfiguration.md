# ProServe::Organizations::Account DeploymentAccountConfiguration

(Optional) A dedicated deployment account can be used to further enhance security. This configuration creates a second role within the newly created account. This role trusts the given deployment account, allowing users in the given deployment account to assume the role.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#accountid" title="AccountId">AccountId</a>" : <i>String</i>,
    "<a href="#rolename" title="RoleName">RoleName</a>" : <i>String</i>,
    "<a href="#awsmanagedpolicyarns" title="AWSManagedPolicyArns">AWSManagedPolicyArns</a>" : <i>[ String, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#accountid" title="AccountId">AccountId</a>: <i>String</i>
<a href="#rolename" title="RoleName">RoleName</a>: <i>String</i>
<a href="#awsmanagedpolicyarns" title="AWSManagedPolicyArns">AWSManagedPolicyArns</a>: <i>
      - String</i>
</pre>

## Properties

#### AccountId

Deployment Account Id

_Required_: Yes

_Type_: String

_Minimum_: <code>12</code>

_Maximum_: <code>12</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RoleName

Deployment Role Name.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>256</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AWSManagedPolicyArns

A List of AWS managed policy arn's to attach to the deployment account role

_Required_: Yes

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

