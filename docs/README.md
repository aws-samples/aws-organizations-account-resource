# ProServe::Organizations::Account

Resource Schema for ProServe::Organizations::Account

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "ProServe::Organizations::Account",
    "Properties" : {
        "<a href="#accountname" title="AccountName">AccountName</a>" : <i>String</i>,
        "<a href="#accountemail" title="AccountEmail">AccountEmail</a>" : <i>String</i>,
        "<a href="#organizationalunitid" title="OrganizationalUnitId">OrganizationalUnitId</a>" : <i>String</i>,
        "<a href="#alternatecontacts" title="AlternateContacts">AlternateContacts</a>" : <i><a href="alternatecontacts.md">AlternateContacts</a></i>,
        "<a href="#organizationaccountaccessrolename" title="OrganizationAccountAccessRoleName">OrganizationAccountAccessRoleName</a>" : <i>String</i>,
        "<a href="#deploymentaccountconfiguration" title="DeploymentAccountConfiguration">DeploymentAccountConfiguration</a>" : <i><a href="deploymentaccountconfiguration.md">DeploymentAccountConfiguration</a></i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#notificationtopicarn" title="NotificationTopicArn">NotificationTopicArn</a>" : <i>String</i>,
        "<a href="#closeaccountondeletion" title="CloseAccountOnDeletion">CloseAccountOnDeletion</a>" : <i>Boolean</i>
    }
}
</pre>

### YAML

<pre>
Type: ProServe::Organizations::Account
Properties:
    <a href="#accountname" title="AccountName">AccountName</a>: <i>String</i>
    <a href="#accountemail" title="AccountEmail">AccountEmail</a>: <i>String</i>
    <a href="#organizationalunitid" title="OrganizationalUnitId">OrganizationalUnitId</a>: <i>String</i>
    <a href="#alternatecontacts" title="AlternateContacts">AlternateContacts</a>: <i><a href="alternatecontacts.md">AlternateContacts</a></i>
    <a href="#organizationaccountaccessrolename" title="OrganizationAccountAccessRoleName">OrganizationAccountAccessRoleName</a>: <i>String</i>
    <a href="#deploymentaccountconfiguration" title="DeploymentAccountConfiguration">DeploymentAccountConfiguration</a>: <i><a href="deploymentaccountconfiguration.md">DeploymentAccountConfiguration</a></i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#notificationtopicarn" title="NotificationTopicArn">NotificationTopicArn</a>: <i>String</i>
    <a href="#closeaccountondeletion" title="CloseAccountOnDeletion">CloseAccountOnDeletion</a>: <i>Boolean</i>
</pre>

## Properties

#### AccountName

The friendly name of the member account.

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AccountEmail

The email address of the owner to assign to the new member account. This email address must not already be associated with another AWS account. You must use a valid email address to complete account creation. You can't access the root user of the account or remove an account that was created with an invalid email address.

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### OrganizationalUnitId

The unique identifier (ID) of the root or organizational unit that you want to create the account in.

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AlternateContacts

(Optional) Alternate contacts to be set

_Required_: No

_Type_: <a href="alternatecontacts.md">AlternateContacts</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### OrganizationAccountAccessRoleName

The name of an IAM role that AWS Organizations automatically preconfigures in the new member account. This role trusts the management account, allowing users in the management account to assume the role, as permitted by the management account administrator. The role has administrator permissions in the new member account.

If you don't specify this parameter, the role name defaults to `OrganizationAccountAccessRole`.

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### DeploymentAccountConfiguration

(Optional) A dedicated deployment account can be used to further enhance security. This configuration creates a second role within the newly created account. This role trusts the given deployment account, allowing users in the given deployment account to assume the role.

_Required_: No

_Type_: <a href="deploymentaccountconfiguration.md">DeploymentAccountConfiguration</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

One or more tags.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NotificationTopicArn

The SNS topic ARN to which to publish failure reasons.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### CloseAccountOnDeletion

If set to true account will be closed by AWS CloudFormation. Otherwise, just parked in root organizational unit.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the AccountId.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### AccountId

The unique identifier (ID) of the account.

#### AccountRequestId

The unique identifier (ID) of the account creation request.

