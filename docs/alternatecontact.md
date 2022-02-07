# ProServe::Organizations::Account AlternateContact

Alternate contact to be set

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#email" title="Email">Email</a>" : <i>String</i>,
    "<a href="#name" title="Name">Name</a>" : <i>String</i>,
    "<a href="#phonenumber" title="PhoneNumber">PhoneNumber</a>" : <i>String</i>,
    "<a href="#title" title="Title">Title</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#email" title="Email">Email</a>: <i>String</i>
<a href="#name" title="Name">Name</a>: <i>String</i>
<a href="#phonenumber" title="PhoneNumber">PhoneNumber</a>: <i>String</i>
<a href="#title" title="Title">Title</a>: <i>String</i>
</pre>

## Properties

#### Email

Contacts email address

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>12</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Name

Contacts name

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>12</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PhoneNumber

Contacts phone number

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>12</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Title

Contacts title

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>12</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

