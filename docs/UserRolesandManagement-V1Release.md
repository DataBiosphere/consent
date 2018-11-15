# User Roles and Management
## Documentation V1 Release

### Admin
* ***Amount of users with this role:*** at least 1.
* ***Role combinations:*** this role can be combined with any other role.
* ***Possible replacement conflicts:*** the only conflict this role might have is if there is only one admin in the system and he/she wants to be removed from this charge. The system will not allow to change the admin role if there is no other admin set.

### DAC Member
* ***Amount of users with this role:*** at least 4.
* ***Role combinations:*** this role can only be combined with the Admin role.
* ***Possible replacement conflicts:*** when trying to edit his/her role to become an alumni or a researcher, just remember that in order to be able to create new elections, the minimal amount of DAC Members must be 4, so it is possible that you may need to add a new DAC Member in the system.
* If trying to edit the user to become a Chairperson, since there can’t be 2 users with that role, the current Chairperson (if there is any in the system) will become an Alumni (see recommended action below).  

### Chairperson
* ***Amount of users with this role:*** only 1.
* ***Role combinations:*** this role can only be combined with the Admin role.
* ***Possible replacement conflicts:***  when trying to edit his/her role to become a member, an alumni or a researcher, just remember that in order to be able to create new elections, another user must become a Chairperson (see recommended action below).

### Alumni
* ***Amount of users with this role:*** undefined.
* ***Role combinations:*** this role can be combined with the Admin and the Researcher role.
* ***Possible replacement conflicts:***  If trying to edit the user to become a Chairperson, since there can’t be 2 users with that role, the current Chairperson (if there is any in the system) will become an Alumni (see recommended action below).

### Researcher
* ***Amount of users with this role:***  undefined.
* ***Role combinations:*** tthis role can be combined with the Admin and the Alumni role.
* ***Possible replacement conflicts:*** If trying to edit the user to become a Chairperson, since there can’t be 2 users with that role, the current Chairperson (if there is any in the system) will become an Alumni (see recommended action below).

>***In order to avoid a huge amount of conflict cases that appear when trying to edit a user role, as a temporary solution for V1 Release, User Role Edition will be allowed only if there are no open elections.***

### Recommended Actions

**Replacing a chairperson**. To avoid conflicts, the admin should follow the following steps when replacing a chairperson:
1. Complete all open elections in the system.
2. Make sure that the new DAC can include at least 4 members (including the chairperson) after the modification you are about to make.
3. In order to replace the Chairperson you can either:
* Create a new user with the role of Chairperson (the current Chairperson will become an alumni. If you want this user to become some other thing you can edit it later of course).
* Replace the role of some other user to become a Chairperson (the current Chairperson will become an alumni. If you want this user to become some other thing you can edit it later of course).
* Replace the role of the current Chairperson to become whatever role you want. But have in consideration that the system won’t allow you to create elections until you give the Chairperson role to some user.