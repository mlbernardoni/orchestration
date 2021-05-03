import uuid
import json
import requests
import time

class R2sLib:
    R2s_TIMEOUT = 600 #sec
    R2s_TIMEOUT_WAIT = 3 #sec
    R2s_TRIES = 3
    R2s_URL = 'http://localhost:8080/R2s/R2s.html'
    r2smsg = None
    r2regarray = list()
   
    def __init__(self, jb =  None):
        if jb is not None:
            self.r2smsg = jb.get("r2_msg")
        
    def R2s_SetTimeout(self, newtimeout):
        self.R2s_TIMEOUT = newtimeout   
        return
 
    def R2s_SetTimeoutWait(self, newwait):
        self.R2s_TIMEOUT_WAIT = newwait  
        return 
 
    def R2s_SetTries(self, newtries):
        self.R2s_TRIES = newtries                   
        return 
 
    def R2s_GetParam(self):
        if self.r2smsg is None: return None
        return self.r2smsg.get('service_param')
        
    def R2s_GetRootID(self):
        if self.r2smsg is None: return None
        return self.r2smsg.get('root_service')      

    def R2s_GetParentID(self):
        if self.r2smsg is None: return None
        return self.r2smsg.get('parent_service') 

    def R2s_GetServiceID(self):
        if self.r2smsg is None: return None
        return self.r2smsg.get('service')    

    def R2s_GetID(self):
        return str(uuid.uuid4()) 
        
    def R2s_Final(self, eventurl, eventparm, serviceid = None, tries = None, timeout = None, timeoutwait = None): 
        return self.R2s_FlowService(eventurl, eventparm, 'F', serviceid, tries, timeout, timeoutwait)

    def R2s_Error(self, eventurl, eventparm, serviceid = None, tries = None, timeout = None, timeoutwait = None): 
        return self.R2s_FlowService(eventurl, eventparm, 'E', serviceid, tries, timeout, timeoutwait)

    def R2s_Subsequent(self, eventurl, eventparm, serviceid = None, tries = None, timeout = None, timeoutwait = None): 
        return self.R2s_FlowService(eventurl, eventparm, 'S', serviceid, tries, timeout, timeoutwait)

    def R2s_Contained(self, eventurl, eventparm, serviceid = None, tries = None, timeout = None, timeoutwait = None): 
        return self.R2s_FlowService(eventurl, eventparm, 'C', serviceid, tries, timeout, timeoutwait)

    def R2s_Independent(self, eventurl, eventparm, serviceid = None, tries = None, timeout = None, timeoutwait = None): 
        return self.R2s_FlowService(eventurl, eventparm, 'I', serviceid, tries, timeout, timeoutwait)
      
    def R2s_FlowService(self, eventurl, eventparm, servicetype, serviceid, tries, timeout, timeoutwait): 
        msgid = str(serviceid)
        if serviceid is None: msgid = self.R2s_GetID()
        rootmsg = {}
        rootmsg['service'] = msgid
        rootmsg['root_service'] = self.R2s_GetRootID()
        rootmsg['parent_service'] = self.R2s_GetServiceID()
        rootmsg['service_url'] = eventurl
        rootmsg['service_param'] = str(eventparm)
        rootmsg['service_type'] = servicetype
        if tries is not None: rootmsg['tries'] = tries
        if timeout is not None: rootmsg['timeout'] = timeout
        if timeoutwait is not None: rootmsg['timeoutwait'] = timeoutwait
        self.r2regarray.append(rootmsg)
        return msgid

    def R2s_Setpredecessor(self, preid, postid): 
        rootmsg = {}
        rootmsg['service_type'] = 'Pre'
        rootmsg['root_service'] = self.R2s_GetRootID()
        rootmsg['pre_service'] = preid
        rootmsg['blocked_service'] = postid
        self.r2regarray.append(rootmsg)
        return 
        
    def R2s_Release(self): 
        rootmsg = {}
        rootmsg['type'] = 'Batch'
        rootmsg['r2_msg'] = self.r2regarray
        rootmsg['root_service'] = self.R2s_GetRootID()
        rootmsg['service'] = self.R2s_GetServiceID()
        self.r2regarray = []
        x = self.R2s_SendEvent(rootmsg)
        return x.text

        
    def R2s_Complete(self): 
        rootmsg = {}
        rootmsg['service_type'] = 'Update'
        rootmsg['root_service'] = self.R2s_GetRootID()
        rootmsg['parent_service'] = self.R2s_GetParentID()
        rootmsg['service'] = self.R2s_GetServiceID()

        rtoossend = {}
        rtoossend['r2_msg'] = rootmsg            
        rtoossend['root_service'] = self.R2s_GetRootID()            
        rtoossend['type'] = 'Complete' 
        x = self.R2s_SendEvent(rtoossend)
        return x.text
    
    def R2s_Root(self, eventurl, eventparm, serviceid = None, tries = None, timeout = None, timeoutwait = None):
        msgid = serviceid
        if serviceid is None: msgid = self.R2s_GetID()
        rootmsg = {}
        rootmsg['service'] = msgid
        rootmsg['service_url'] = eventurl
        rootmsg['service_param'] = eventparm
        rootmsg['service_type'] = 'Root'
        if tries is not None: rootmsg['tries'] = tries
        if timeout is not None: rootmsg['timeout'] = timeout
        if timeoutwait is not None: rootmsg['timeoutwait'] = timeoutwait

        rtoossend = {}
        rtoossend['r2_msg'] = rootmsg            
        rtoossend['root_service'] = msgid            
        rtoossend['type'] = 'Root' 
        x = self.R2s_SendEvent(rtoossend)
        return x.text
        
    def R2s_SendEvent(self, rtoossend):
        trys = self.R2s_TRIES
        errorstr = 'R2sLib Error: '
        while trys > 0 :
            try:
                x = requests.post(self.R2s_URL, json = rtoossend, timeout = self.R2s_TIMEOUT)
                if x.status_code == 200: return x
                else :
                    errorstr = errorstr + str(x.status_code) + ' '
            except requests.exceptions.RequestException as e:
                # add wait here
                time.sleep( self.R2s_TIMEOUT_WAIT )
                errorstr = errorstr + e + ' '
            trys = trys - 1
        print(errorstr)
        raise SystemExit(errorstr)

    