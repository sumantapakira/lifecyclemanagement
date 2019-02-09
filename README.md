Use case: Life Cycle Management System

Problem statement:
1. Marketers or content authors wants to have a solution where they can quickly define the state of an asset or a page. A state of an asset or page defines the
business process or a business decision. Let’s assume, an asset or page needs to go through several workflow approval steps before its become approved version. The problem
in this process is that marketers or content authors does not know the actual state of the page or asset. In order to show the actual status, some customisation required on top
of out-of-the box AEM which means development effort required and also marketers does not have flexibility to define their own states.
2. When content reviewers are reviewing the page or asset during the workflow process then content authors can create a new version which makes the work harder
for reviewer to review.
3. Marketers or content authors needs a tool make quick decision as per the business rule. AEM workflow engine does this work but it does not gives the status
of a page or asset in the GUI to make a quick decision. Some third party tools can be integrated but it comes with additional cost.
4. Marketers want to have permission based tool which allows to apply business process for quick decision.
5. If content author rolls out a page then the child page gets overriden even when the work in progress on that child page.

Solution:
Life cycle management tool is a feature which signifies the different states of a page or an assets. User can define their own life cycle model and states and apply those states during various workflow process or apply directly with proper permission.
It provides an editor which allows to marketers to design life cycle state by simple drag and drop options. For example, one can define one life cycle model with
the states as DRAFT, UNDER REVIEW, and APPROVED and someone also can define more complex life cycle model states such as, SUSPENDED, DRAFT, APPROVED-1, APPROVED-2
for example where the flow can be possible from DRAFT <->APPROVED-1 <-> APPROVED-2 or SUSPENDED <-> DRAFT. The life cycle management engine allows bidirectional
flow from one state to another by Promote, Demote, Suspend or Resume. A suspended flow cannot be promoted to Approved for example.

Life cycle management editor allows marketers to define the color for each life cycle state so that when a page or asset move from one state to another then
it shows the corresponding color in Touch UI interface. This gives the ability for marketers to see the status all at a glance.

Life cycle becomes more powerful in association with workflow engine. This functionality can be integrated with AEM workflow engine or can be used independently with proper permission.
Someone can configure AEM workflow engine with Life cycle tool seamlessly.
