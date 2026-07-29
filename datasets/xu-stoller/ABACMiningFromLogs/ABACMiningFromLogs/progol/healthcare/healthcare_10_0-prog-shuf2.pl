%Settings
:- set(nodes,1612)?
:- set(h,100)?
:- set(posonly)?
%Mode Declarations
:- modeh(1,up(+user,+resource,#operation))?
:- modeb(1,positionU(+user,#positionUType))?
:- modeb(1,uidU(+user,#uidUType))?
:- modeb(*,teamsU(+user,#teamsUType))?
:- modeb(*,specialtiesU(+user,#specialtiesUType))?
:- modeb(1,wardU(+user,#wardUType))?
:- modeb(*,agentForU(+user,#agentForUType))?
:- modeb(1,treatingTeamR(+resource,#treatingTeamRType))?
:- modeb(1,authorR(+resource,#authorRType))?
:- modeb(1,topicsR(+resource,#topicsRType))?
:- modeb(1,patientR(+resource,#patientRType))?
:- modeb(1,wardR(+resource,#wardRType))?
:- modeb(1,ridR(+resource,#ridRType))?
:- modeb(1,typeR(+resource,#typeRType))?
:- modeb(1,teamsU_superset_topicsR(+user,+resource))?
:- modeb(1,specialtiesU_superset_topicsR(+user,+resource))?
:- modeb(1,agentForU_superset_topicsR(+user,+resource))?
:- modeb(1,teamsU_contains_treatingTeamR(+user,+resource))?
:- modeb(1,teamsU_contains_authorR(+user,+resource))?
:- modeb(1,teamsU_contains_patientR(+user,+resource))?
:- modeb(1,teamsU_contains_wardR(+user,+resource))?
:- modeb(1,teamsU_contains_ridR(+user,+resource))?
:- modeb(1,teamsU_contains_typeR(+user,+resource))?
:- modeb(1,specialtiesU_contains_treatingTeamR(+user,+resource))?
:- modeb(1,specialtiesU_contains_authorR(+user,+resource))?
:- modeb(1,specialtiesU_contains_patientR(+user,+resource))?
:- modeb(1,specialtiesU_contains_wardR(+user,+resource))?
:- modeb(1,specialtiesU_contains_ridR(+user,+resource))?
:- modeb(1,specialtiesU_contains_typeR(+user,+resource))?
:- modeb(1,agentForU_contains_treatingTeamR(+user,+resource))?
:- modeb(1,agentForU_contains_authorR(+user,+resource))?
:- modeb(1,agentForU_contains_patientR(+user,+resource))?
:- modeb(1,agentForU_contains_wardR(+user,+resource))?
:- modeb(1,agentForU_contains_ridR(+user,+resource))?
:- modeb(1,agentForU_contains_typeR(+user,+resource))?
:- modeb(1,positionU_equals_treatingTeamR(+user,+resource))?
:- modeb(1,positionU_equals_authorR(+user,+resource))?
:- modeb(1,positionU_equals_patientR(+user,+resource))?
:- modeb(1,positionU_equals_wardR(+user,+resource))?
:- modeb(1,positionU_equals_ridR(+user,+resource))?
:- modeb(1,positionU_equals_typeR(+user,+resource))?
:- modeb(1,uidU_equals_treatingTeamR(+user,+resource))?
:- modeb(1,uidU_equals_authorR(+user,+resource))?
:- modeb(1,uidU_equals_patientR(+user,+resource))?
:- modeb(1,uidU_equals_wardR(+user,+resource))?
:- modeb(1,uidU_equals_ridR(+user,+resource))?
:- modeb(1,uidU_equals_typeR(+user,+resource))?
:- modeb(1,wardU_equals_treatingTeamR(+user,+resource))?
:- modeb(1,wardU_equals_authorR(+user,+resource))?
:- modeb(1,wardU_equals_patientR(+user,+resource))?
:- modeb(1,wardU_equals_wardR(+user,+resource))?
:- modeb(1,wardU_equals_ridR(+user,+resource))?
:- modeb(1,wardU_equals_typeR(+user,+resource))?
:- modeb(1,superset(+attribValSet,+attribValSet))?
:- modeb(1,element(+attribValAtomic,+attribValSet))?
:- modeb(1,element(+attribValAtomic,#attribValSet))?
:- modeb(1,element(#attribValAtomic,+attribValSet))?
%Types
user(oncNurse1).
user(oncNurse2).
user(oncDoc4).
user(carDoc2).
user(carDoc1).
user(oncAgent1).
user(anesDoc1).
user(oncAgent2).
user(oncDoc1).
user(oncDoc3).
user(oncDoc2).
user(oncPat1).
user(oncPat2).
user(carAgent1).
user(carNurse1).
user(carAgent2).
user(carNurse2).
user(carPat1).
user(doc2).
user(carPat2).
user(doc1).
resource(oncPat2oncItem).
resource(oncPat2noteItem).
resource(carPat1HR).
resource(oncPat1noteItem).
resource(oncPat2nursingItem).
resource(carPat2nursingItem).
resource(carPat1noteItem).
resource(carPat2carItem).
resource(oncPat1nursingItem).
resource(oncPat1oncItem).
resource(carPat1carItem).
resource(oncPat2HR).
resource(carPat1nursingItem).
resource(oncPat1HR).
resource(carPat2HR).
resource(carPat2noteItem).
operation(read).
operation(addItem).
operation(addNote).
attribValAtomic(oncDoc4).
attribValAtomic(HR).
attribValAtomic(anesthesiology).
attribValAtomic(oncWard).
attribValAtomic(carPat2nursingItem).
attribValAtomic(doctor).
attribValAtomic(carPat1noteItem).
attribValAtomic(carPat2carItem).
attribValAtomic(oncPat1nursingItem).
attribValAtomic(nurse).
attribValAtomic(carPat1nursingItem).
attribValAtomic(carNurse1).
attribValAtomic(carAgent1).
attribValAtomic(carNurse2).
attribValAtomic(carAgent2).
attribValAtomic(carPat2noteItem).
attribValAtomic(pediatrics).
attribValAtomic(oncNurse1).
attribValAtomic(oncNurse2).
attribValAtomic(cardiology).
attribValAtomic(carPat1HR).
attribValAtomic(oncAgent1).
attribValAtomic(carTeam2).
attribValAtomic(oncAgent2).
attribValAtomic(carTeam1).
attribValAtomic(oncPat2HR).
attribValAtomic(oncPat1HR).
attribValAtomic(carPat2HR).
attribValAtomic(oncPat2oncItem).
attribValAtomic(HRitem).
attribValAtomic(neurology).
attribValAtomic(oncPat2noteItem).
attribValAtomic(oncPat2nursingItem).
attribValAtomic(oncPat1oncItem).
attribValAtomic(carPat1carItem).
attribValAtomic(carPat1).
attribValAtomic(carPat2).
attribValAtomic(note).
attribValAtomic(carWard).
attribValAtomic(carDoc2).
attribValAtomic(carDoc1).
attribValAtomic(oncPat1noteItem).
attribValAtomic(oncology).
attribValAtomic(anesDoc1).
attribValAtomic(oncDoc1).
attribValAtomic(oncDoc3).
attribValAtomic(oncDoc2).
attribValAtomic(oncPat1).
attribValAtomic(oncTeam2).
attribValAtomic(oncTeam1).
attribValAtomic(oncPat2).
attribValAtomic(doc2).
attribValAtomic(nursing).
attribValAtomic(doc1).
attribValSet([]).
attribValSet([V|Vs]) :- attribValAtomic(V), attribValSet(Vs).
positionUType(nurse).
positionUType(doctor).
uidUType(oncNurse1).
uidUType(oncNurse2).
uidUType(oncDoc4).
uidUType(carDoc2).
uidUType(carDoc1).
uidUType(oncAgent1).
uidUType(anesDoc1).
uidUType(oncAgent2).
uidUType(oncDoc1).
uidUType(oncDoc3).
uidUType(oncDoc2).
uidUType(oncPat1).
uidUType(oncPat2).
uidUType(carAgent1).
uidUType(carNurse1).
uidUType(carAgent2).
uidUType(carNurse2).
uidUType(carPat1).
uidUType(doc2).
uidUType(carPat2).
uidUType(doc1).
teamsUType(oncTeam1).
teamsUType(carTeam1).
teamsUType(oncTeam2).
teamsUType(oncTeam1).
teamsUType(carTeam2).
teamsUType(oncTeam2).
teamsUType(oncTeam1).
teamsUType(carTeam1).
specialtiesUType(cardiology).
specialtiesUType(oncology).
specialtiesUType(anesthesiology).
specialtiesUType(pediatrics).
specialtiesUType(oncology).
specialtiesUType(cardiology).
specialtiesUType(neurology).
wardUType(carWard).
wardUType(oncWard).
agentForUType(oncPat2).
agentForUType(carPat2).
treatingTeamRType(oncTeam2).
treatingTeamRType(oncTeam1).
treatingTeamRType(carTeam2).
treatingTeamRType(carTeam1).
authorRType(oncDoc1).
authorRType(oncNurse1).
authorRType(oncNurse2).
authorRType(carDoc2).
authorRType(oncPat1).
authorRType(carAgent1).
authorRType(carNurse1).
authorRType(carNurse2).
authorRType(doc2).
authorRType(carPat1).
authorRType(oncAgent1).
authorRType(doc1).
topicsRType(cardiology).
topicsRType(oncology).
topicsRType(nursing).
topicsRType(note).
patientRType(oncPat1).
patientRType(oncPat2).
patientRType(carPat1).
patientRType(carPat2).
wardRType(carWard).
wardRType(oncWard).
ridRType(oncPat2oncItem).
ridRType(oncPat2noteItem).
ridRType(carPat1HR).
ridRType(oncPat1noteItem).
ridRType(oncPat2nursingItem).
ridRType(carPat2nursingItem).
ridRType(carPat1noteItem).
ridRType(carPat2carItem).
ridRType(oncPat1nursingItem).
ridRType(oncPat1oncItem).
ridRType(carPat1carItem).
ridRType(oncPat2HR).
ridRType(carPat1nursingItem).
ridRType(oncPat1HR).
ridRType(carPat2HR).
ridRType(carPat2noteItem).
typeRType(HRitem).
typeRType(HR).
%Background Knowledge
positionU(oncNurse1,nurse).
uidU(oncNurse1,oncNurse1).
wardU(oncNurse1,oncWard).
positionU(oncNurse2,nurse).
uidU(oncNurse2,oncNurse2).
wardU(oncNurse2,oncWard).
positionU(oncDoc4,doctor).
uidU(oncDoc4,oncDoc4).
teamsU(oncDoc4,oncTeam2).
specialtiesU(oncDoc4,oncology).
positionU(carDoc2,doctor).
uidU(carDoc2,carDoc2).
teamsU(carDoc2,carTeam2).
specialtiesU(carDoc2,cardiology).
positionU(carDoc1,doctor).
uidU(carDoc1,carDoc1).
teamsU(carDoc1,carTeam1).
specialtiesU(carDoc1,cardiology).
uidU(oncAgent1,oncAgent1).
agentForU(oncAgent1,oncPat2).
positionU(anesDoc1,doctor).
uidU(anesDoc1,anesDoc1).
teamsU(anesDoc1,oncTeam1).
teamsU(anesDoc1,carTeam1).
specialtiesU(anesDoc1,anesthesiology).
uidU(oncAgent2,oncAgent2).
agentForU(oncAgent2,oncPat2).
positionU(oncDoc1,doctor).
uidU(oncDoc1,oncDoc1).
teamsU(oncDoc1,oncTeam2).
teamsU(oncDoc1,oncTeam1).
specialtiesU(oncDoc1,oncology).
positionU(oncDoc3,doctor).
uidU(oncDoc3,oncDoc3).
teamsU(oncDoc3,oncTeam2).
specialtiesU(oncDoc3,oncology).
positionU(oncDoc2,doctor).
uidU(oncDoc2,oncDoc2).
teamsU(oncDoc2,oncTeam1).
specialtiesU(oncDoc2,oncology).
uidU(oncPat1,oncPat1).
wardU(oncPat1,oncWard).
uidU(oncPat2,oncPat2).
wardU(oncPat2,oncWard).
uidU(carAgent1,carAgent1).
agentForU(carAgent1,carPat2).
positionU(carNurse1,nurse).
uidU(carNurse1,carNurse1).
wardU(carNurse1,carWard).
uidU(carAgent2,carAgent2).
agentForU(carAgent2,carPat2).
positionU(carNurse2,nurse).
uidU(carNurse2,carNurse2).
wardU(carNurse2,carWard).
uidU(carPat1,carPat1).
wardU(carPat1,carWard).
positionU(doc2,doctor).
uidU(doc2,doc2).
specialtiesU(doc2,cardiology).
specialtiesU(doc2,neurology).
uidU(carPat2,carPat2).
wardU(carPat2,carWard).
positionU(doc1,doctor).
uidU(doc1,doc1).
specialtiesU(doc1,pediatrics).
specialtiesU(doc1,oncology).
treatingTeamR(oncPat2oncItem,oncTeam2).
authorR(oncPat2oncItem,doc1).
topicsR(oncPat2oncItem,oncology).
patientR(oncPat2oncItem,oncPat2).
wardR(oncPat2oncItem,oncWard).
ridR(oncPat2oncItem,oncPat2oncItem).
typeR(oncPat2oncItem,HRitem).
treatingTeamR(oncPat2noteItem,oncTeam2).
authorR(oncPat2noteItem,oncAgent1).
topicsR(oncPat2noteItem,note).
patientR(oncPat2noteItem,oncPat2).
wardR(oncPat2noteItem,oncWard).
ridR(oncPat2noteItem,oncPat2noteItem).
typeR(oncPat2noteItem,HRitem).
treatingTeamR(carPat1HR,carTeam1).
patientR(carPat1HR,carPat1).
wardR(carPat1HR,carWard).
ridR(carPat1HR,carPat1HR).
typeR(carPat1HR,HR).
treatingTeamR(oncPat1noteItem,oncTeam1).
authorR(oncPat1noteItem,oncPat1).
topicsR(oncPat1noteItem,note).
patientR(oncPat1noteItem,oncPat1).
wardR(oncPat1noteItem,oncWard).
ridR(oncPat1noteItem,oncPat1noteItem).
typeR(oncPat1noteItem,HRitem).
treatingTeamR(oncPat2nursingItem,oncTeam2).
authorR(oncPat2nursingItem,oncNurse1).
topicsR(oncPat2nursingItem,nursing).
patientR(oncPat2nursingItem,oncPat2).
wardR(oncPat2nursingItem,oncWard).
ridR(oncPat2nursingItem,oncPat2nursingItem).
typeR(oncPat2nursingItem,HRitem).
treatingTeamR(carPat2nursingItem,carTeam2).
authorR(carPat2nursingItem,carNurse2).
topicsR(carPat2nursingItem,nursing).
patientR(carPat2nursingItem,carPat2).
wardR(carPat2nursingItem,carWard).
ridR(carPat2nursingItem,carPat2nursingItem).
typeR(carPat2nursingItem,HRitem).
treatingTeamR(carPat1noteItem,carTeam1).
authorR(carPat1noteItem,carPat1).
topicsR(carPat1noteItem,note).
patientR(carPat1noteItem,carPat1).
wardR(carPat1noteItem,carWard).
ridR(carPat1noteItem,carPat1noteItem).
typeR(carPat1noteItem,HRitem).
treatingTeamR(carPat2carItem,carTeam2).
authorR(carPat2carItem,doc2).
topicsR(carPat2carItem,cardiology).
patientR(carPat2carItem,carPat2).
wardR(carPat2carItem,carWard).
ridR(carPat2carItem,carPat2carItem).
typeR(carPat2carItem,HRitem).
treatingTeamR(oncPat1nursingItem,oncTeam1).
authorR(oncPat1nursingItem,oncNurse2).
topicsR(oncPat1nursingItem,nursing).
patientR(oncPat1nursingItem,oncPat1).
wardR(oncPat1nursingItem,oncWard).
ridR(oncPat1nursingItem,oncPat1nursingItem).
typeR(oncPat1nursingItem,HRitem).
treatingTeamR(oncPat1oncItem,oncTeam1).
authorR(oncPat1oncItem,oncDoc1).
topicsR(oncPat1oncItem,oncology).
patientR(oncPat1oncItem,oncPat1).
wardR(oncPat1oncItem,oncWard).
ridR(oncPat1oncItem,oncPat1oncItem).
typeR(oncPat1oncItem,HRitem).
treatingTeamR(carPat1carItem,carTeam1).
authorR(carPat1carItem,carDoc2).
topicsR(carPat1carItem,cardiology).
patientR(carPat1carItem,carPat1).
wardR(carPat1carItem,carWard).
ridR(carPat1carItem,carPat1carItem).
typeR(carPat1carItem,HRitem).
treatingTeamR(oncPat2HR,oncTeam2).
patientR(oncPat2HR,oncPat2).
wardR(oncPat2HR,oncWard).
ridR(oncPat2HR,oncPat2HR).
typeR(oncPat2HR,HR).
treatingTeamR(carPat1nursingItem,carTeam1).
authorR(carPat1nursingItem,carNurse1).
topicsR(carPat1nursingItem,nursing).
patientR(carPat1nursingItem,carPat1).
wardR(carPat1nursingItem,carWard).
ridR(carPat1nursingItem,carPat1nursingItem).
typeR(carPat1nursingItem,HRitem).
treatingTeamR(oncPat1HR,oncTeam1).
patientR(oncPat1HR,oncPat1).
wardR(oncPat1HR,oncWard).
ridR(oncPat1HR,oncPat1HR).
typeR(oncPat1HR,HR).
treatingTeamR(carPat2HR,carTeam2).
patientR(carPat2HR,carPat2).
wardR(carPat2HR,carWard).
ridR(carPat2HR,carPat2HR).
typeR(carPat2HR,HR).
treatingTeamR(carPat2noteItem,carTeam2).
authorR(carPat2noteItem,carAgent1).
topicsR(carPat2noteItem,note).
patientR(carPat2noteItem,carPat2).
wardR(carPat2noteItem,carWard).
ridR(carPat2noteItem,carPat2noteItem).
typeR(carPat2noteItem,HRitem).
teamsU_superset_topicsR(U,R) :- setof(X,teamsU(U,X),SU), setof(Y,topicsR(R,Y),SR), superset(SU,SR), not(SR==[]).
specialtiesU_superset_topicsR(U,R) :- setof(X,specialtiesU(U,X),SU), setof(Y,topicsR(R,Y),SR), superset(SU,SR), not(SR==[]).
agentForU_superset_topicsR(U,R) :- setof(X,agentForU(U,X),SU), setof(Y,topicsR(R,Y),SR), superset(SU,SR), not(SR==[]).
teamsU_contains_treatingTeamR(U,R) :-teamsU(U,X),treatingTeamR(R,X).
teamsU_contains_authorR(U,R) :-teamsU(U,X),authorR(R,X).
teamsU_contains_patientR(U,R) :-teamsU(U,X),patientR(R,X).
teamsU_contains_wardR(U,R) :-teamsU(U,X),wardR(R,X).
teamsU_contains_ridR(U,R) :-teamsU(U,X),ridR(R,X).
teamsU_contains_typeR(U,R) :-teamsU(U,X),typeR(R,X).
specialtiesU_contains_treatingTeamR(U,R) :-specialtiesU(U,X),treatingTeamR(R,X).
specialtiesU_contains_authorR(U,R) :-specialtiesU(U,X),authorR(R,X).
specialtiesU_contains_patientR(U,R) :-specialtiesU(U,X),patientR(R,X).
specialtiesU_contains_wardR(U,R) :-specialtiesU(U,X),wardR(R,X).
specialtiesU_contains_ridR(U,R) :-specialtiesU(U,X),ridR(R,X).
specialtiesU_contains_typeR(U,R) :-specialtiesU(U,X),typeR(R,X).
agentForU_contains_treatingTeamR(U,R) :-agentForU(U,X),treatingTeamR(R,X).
agentForU_contains_authorR(U,R) :-agentForU(U,X),authorR(R,X).
agentForU_contains_patientR(U,R) :-agentForU(U,X),patientR(R,X).
agentForU_contains_wardR(U,R) :-agentForU(U,X),wardR(R,X).
agentForU_contains_ridR(U,R) :-agentForU(U,X),ridR(R,X).
agentForU_contains_typeR(U,R) :-agentForU(U,X),typeR(R,X).
positionU_equals_treatingTeamR(U,R) :-positionU(U,X),treatingTeamR(R,X).
positionU_equals_authorR(U,R) :-positionU(U,X),authorR(R,X).
positionU_equals_patientR(U,R) :-positionU(U,X),patientR(R,X).
positionU_equals_wardR(U,R) :-positionU(U,X),wardR(R,X).
positionU_equals_ridR(U,R) :-positionU(U,X),ridR(R,X).
positionU_equals_typeR(U,R) :-positionU(U,X),typeR(R,X).
uidU_equals_treatingTeamR(U,R) :-uidU(U,X),treatingTeamR(R,X).
uidU_equals_authorR(U,R) :-uidU(U,X),authorR(R,X).
uidU_equals_patientR(U,R) :-uidU(U,X),patientR(R,X).
uidU_equals_wardR(U,R) :-uidU(U,X),wardR(R,X).
uidU_equals_ridR(U,R) :-uidU(U,X),ridR(R,X).
uidU_equals_typeR(U,R) :-uidU(U,X),typeR(R,X).
wardU_equals_treatingTeamR(U,R) :-wardU(U,X),treatingTeamR(R,X).
wardU_equals_authorR(U,R) :-wardU(U,X),authorR(R,X).
wardU_equals_patientR(U,R) :-wardU(U,X),patientR(R,X).
wardU_equals_wardR(U,R) :-wardU(U,X),wardR(R,X).
wardU_equals_ridR(U,R) :-wardU(U,X),ridR(R,X).
wardU_equals_typeR(U,R) :-wardU(U,X),typeR(R,X).
superset(Y,[A|X]) :- element(A,Y), superset(Y,X).
superset(Y,[]).
% Positive examples
up(anesDoc1,carPat1HR,addItem).
up(oncPat1,oncPat1noteItem,read).
up(carAgent1,carPat2HR,addNote).
up(carDoc2,carPat2carItem,read).
up(carNurse1,carPat2nursingItem,read).
up(oncPat2,oncPat2noteItem,read).
up(carDoc2,carPat1carItem,read).
up(oncNurse1,oncPat2HR,addItem).
up(oncDoc2,oncPat1oncItem,read).
up(oncNurse1,oncPat2nursingItem,read).
up(carPat1,carPat1HR,addNote).
up(carNurse2,carPat2HR,addItem).
up(oncDoc3,oncPat2HR,addItem).
up(carAgent2,carPat2noteItem,read).
up(carNurse1,carPat1HR,addItem).
up(oncDoc1,oncPat2HR,addItem).
up(anesDoc1,carPat1HR,addItem).
up(carAgent1,carPat2noteItem,read).
up(anesDoc1,oncPat1HR,addItem).
up(oncAgent2,oncPat2HR,addNote).
up(oncNurse1,oncPat2nursingItem,read).
up(oncDoc3,oncPat2HR,addItem).
up(carDoc2,carPat2carItem,read).
up(carPat2,carPat2HR,addNote).
up(oncDoc1,oncPat2HR,addItem).
up(oncDoc4,oncPat2oncItem,read).
up(oncDoc1,oncPat2HR,addItem).
up(carPat2,carPat2HR,addNote).
up(carDoc1,carPat1carItem,read).
up(oncDoc2,oncPat1oncItem,read).
up(oncPat2,oncPat2HR,addNote).
up(carAgent2,carPat2noteItem,read).
up(carPat1,carPat1noteItem,read).
up(oncAgent1,oncPat2HR,addNote).
up(oncDoc3,oncPat2HR,addItem).
up(carNurse1,carPat1HR,addItem).
up(carDoc1,carPat1carItem,read).
up(carNurse2,carPat1HR,addItem).
up(anesDoc1,carPat1HR,addItem).
up(doc2,carPat2carItem,read).
up(carNurse2,carPat2HR,addItem).
up(oncPat1,oncPat1HR,addNote).
up(oncNurse2,oncPat2nursingItem,read).
up(doc1,oncPat2oncItem,read).
up(carNurse2,carPat2nursingItem,read).
up(oncAgent1,oncPat2HR,addNote).
up(carDoc2,carPat2HR,addItem).
up(oncNurse2,oncPat1HR,addItem).
up(oncDoc2,oncPat1HR,addItem).
up(carNurse2,carPat1nursingItem,read).
up(oncNurse1,oncPat1HR,addItem).
up(oncDoc1,oncPat1HR,addItem).
up(carPat1,carPat1HR,addNote).
up(oncNurse2,oncPat2nursingItem,read).
up(oncPat1,oncPat1HR,addNote).
up(oncPat1,oncPat1noteItem,read).
up(doc1,oncPat2oncItem,read).
up(oncAgent1,oncPat2HR,addNote).
up(oncAgent1,oncPat2noteItem,read).
up(oncPat2,oncPat2noteItem,read).
up(oncDoc4,oncPat2HR,addItem).
up(carPat1,carPat1noteItem,read).
up(oncDoc1,oncPat2oncItem,read).
up(oncPat1,oncPat1noteItem,read).
up(carNurse2,carPat2nursingItem,read).
up(oncDoc1,oncPat1oncItem,read).
up(carNurse1,carPat2HR,addItem).
up(carNurse1,carPat1HR,addItem).
up(carAgent1,carPat2noteItem,read).
up(oncDoc4,oncPat2oncItem,read).
up(oncNurse2,oncPat2HR,addItem).
up(carNurse1,carPat2nursingItem,read).
up(carPat2,carPat2noteItem,read).
up(carNurse1,carPat1nursingItem,read).
up(oncAgent2,oncPat2noteItem,read).
up(carNurse1,carPat1nursingItem,read).
up(oncAgent2,oncPat2HR,addNote).
up(carDoc2,carPat2carItem,read).
up(carDoc2,carPat2HR,addItem).
up(oncNurse2,oncPat1nursingItem,read).
up(oncAgent2,oncPat2HR,addNote).
up(carDoc1,carPat1HR,addItem).
up(oncNurse1,oncPat2nursingItem,read).
up(anesDoc1,oncPat1HR,addItem).
up(carDoc2,carPat1carItem,read).
up(anesDoc1,carPat1HR,addItem).
up(carNurse1,carPat1HR,addItem).
up(carPat2,carPat2noteItem,read).
up(oncNurse2,oncPat1HR,addItem).
up(oncNurse2,oncPat2HR,addItem).
up(oncNurse1,oncPat1HR,addItem).
up(carPat2,carPat2noteItem,read).
up(carAgent2,carPat2HR,addNote).
up(carAgent2,carPat2noteItem,read).
up(anesDoc1,oncPat1HR,addItem).
up(oncNurse1,oncPat1nursingItem,read).
up(carPat1,carPat1HR,addNote).
up(carAgent1,carPat2HR,addNote).
up(carNurse1,carPat2HR,addItem).
up(oncNurse2,oncPat1HR,addItem).
up(doc1,oncPat2oncItem,read).
up(anesDoc1,oncPat1HR,addItem).
up(oncPat2,oncPat2HR,addNote).
up(oncNurse1,oncPat1HR,addItem).
up(oncDoc3,oncPat2oncItem,read).
up(oncNurse2,oncPat1nursingItem,read).
up(oncPat2,oncPat2HR,addNote).
up(carNurse2,carPat2nursingItem,read).
up(oncDoc1,oncPat1HR,addItem).
up(doc1,oncPat2oncItem,read).
up(carNurse1,carPat1nursingItem,read).
up(oncDoc1,oncPat2HR,addItem).
up(oncDoc2,oncPat1oncItem,read).
up(oncDoc1,oncPat1oncItem,read).
up(carAgent1,carPat2noteItem,read).
up(oncNurse2,oncPat2nursingItem,read).
up(oncPat2,oncPat2HR,addNote).
up(oncNurse1,oncPat2HR,addItem).
up(oncNurse1,oncPat1nursingItem,read).
up(carPat2,carPat2noteItem,read).
up(oncNurse1,oncPat1nursingItem,read).
up(oncAgent2,oncPat2noteItem,read).
up(oncDoc1,oncPat1HR,addItem).
up(oncNurse1,oncPat1nursingItem,read).
up(anesDoc1,carPat1HR,addItem).
up(oncDoc2,oncPat1HR,addItem).
up(oncNurse2,oncPat1nursingItem,read).
up(oncDoc4,oncPat2oncItem,read).
up(carNurse1,carPat1nursingItem,read).
up(carDoc2,carPat2HR,addItem).
up(oncNurse1,oncPat2nursingItem,read).
up(carAgent2,carPat2noteItem,read).
up(oncNurse1,oncPat2nursingItem,read).
up(carDoc1,carPat1carItem,read).
up(oncDoc2,oncPat1oncItem,read).
up(oncAgent1,oncPat2HR,addNote).
up(doc1,oncPat2oncItem,read).
up(carNurse2,carPat2HR,addItem).
up(oncDoc2,oncPat1oncItem,read).
up(oncDoc2,oncPat1HR,addItem).
up(carDoc1,carPat1HR,addItem).
up(carDoc1,carPat1HR,addItem).
up(oncNurse1,oncPat2HR,addItem).
up(oncDoc3,oncPat2HR,addItem).
up(oncDoc3,oncPat2HR,addItem).
up(oncNurse2,oncPat1nursingItem,read).
up(carNurse1,carPat2HR,addItem).
up(carNurse2,carPat1nursingItem,read).
up(oncNurse2,oncPat1HR,addItem).
up(carNurse2,carPat1nursingItem,read).
up(carDoc1,carPat1HR,addItem).
up(oncNurse1,oncPat2HR,addItem).
up(carNurse2,carPat2HR,addItem).
up(oncPat2,oncPat2noteItem,read).
up(oncDoc4,oncPat2HR,addItem).
up(oncPat1,oncPat1noteItem,read).
up(carAgent2,carPat2noteItem,read).
up(oncDoc1,oncPat2HR,addItem).
up(oncPat1,oncPat1noteItem,read).
up(carNurse1,carPat2HR,addItem).
up(oncAgent1,oncPat2noteItem,read).
up(oncPat1,oncPat1HR,addNote).
up(carDoc2,carPat1carItem,read).
up(carPat1,carPat1noteItem,read).
up(carNurse2,carPat1nursingItem,read).
up(anesDoc1,oncPat1HR,addItem).
up(carNurse1,carPat2HR,addItem).
up(oncNurse1,oncPat1HR,addItem).
up(carNurse1,carPat1nursingItem,read).
up(carNurse2,carPat2nursingItem,read).
up(carNurse2,carPat1HR,addItem).
up(oncAgent1,oncPat2noteItem,read).
up(oncPat1,oncPat1HR,addNote).
up(oncNurse1,oncPat2HR,addItem).
up(oncNurse2,oncPat2HR,addItem).
up(oncDoc4,oncPat2oncItem,read).
up(oncNurse1,oncPat1HR,addItem).
up(carDoc2,carPat2HR,addItem).
up(carPat2,carPat2noteItem,read).
up(carNurse2,carPat2HR,addItem).
up(oncDoc1,oncPat1HR,addItem).
up(doc2,carPat2carItem,read).
up(carNurse2,carPat2nursingItem,read).
up(oncAgent1,oncPat2HR,addNote).
up(carNurse1,carPat2nursingItem,read).
up(carNurse1,carPat2nursingItem,read).
up(carDoc2,carPat2HR,addItem).
up(carDoc2,carPat1carItem,read).
up(carAgent1,carPat2HR,addNote).
up(oncAgent1,oncPat2noteItem,read).
up(carAgent1,carPat2HR,addNote).
up(oncDoc1,oncPat1HR,addItem).
up(carPat1,carPat1noteItem,read).
up(carPat1,carPat1noteItem,read).
up(carPat2,carPat2HR,addNote).
up(oncDoc2,oncPat1HR,addItem).
up(carNurse1,carPat1HR,addItem).
up(oncAgent2,oncPat2HR,addNote).
up(oncNurse1,oncPat1nursingItem,read).
up(carNurse2,carPat1HR,addItem).
up(carNurse1,carPat2nursingItem,read).
up(carDoc1,carPat1carItem,read).
up(carDoc1,carPat1HR,addItem).
up(oncNurse2,oncPat1nursingItem,read).
up(carDoc1,carPat1carItem,read).
