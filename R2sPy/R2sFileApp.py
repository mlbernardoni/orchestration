import json
import time
import uuid
import flask
import flask_restplus
from flask import request, jsonify, render_template
from flask_restplus import Resource, Api, fields
from R2sLib import R2sLib
from cassandra import ConsistencyLevel
from cassandra.cluster import Cluster, BatchStatement
from cassandra.query import SimpleStatement


#app = flask.Flask(__name__)
flask_app = flask.Flask(__name__)
app = Api(app=flask_app)

r2api = app.namespace('Rtoos', description='R2 APIs')

#app.config["DEBUG"] = True
cluster = Cluster(['127.0.0.1'])

input = r2api.model('Resource', {'FileName': fields.String, 
				   'Authenticate' : fields.String,
				   'Clearing' : fields.String })


@r2api.route('/R2sSearch')
class R2sSearch(Resource):
	@r2api.expect(input)
	def post(self):
		return render_template('R2sSearch.html')
	
#def R2sSearch():
###    return render_template('R2sSearch.html')
   
@r2api.route('/R2sUpload')
class R2sUpload(Resource):
	@r2api.expect(input)
	def post(self):
		return render_template('R2sUpload.html')

#def R2sUpload(Resource):
#    return render_template('R2sUpload.html')


    
@r2api.route('/R2sDashboard')
class R2sDashboard(Resource):
	@r2api.expect(input)
	def post(self):
		return render_template('R2sDashboard.html')

#def R2sDashboard():
#    return render_template('R2sDashboard.html')


  
@r2api.route('/R2sProxy')
class R2sProxy(Resource):
	@r2api.expect(input)
	def post(self, input): 
		#input = json.loads(request.data)
		#input = request.json
		r2slib = R2sLib()
		ret = r2slib.R2s_SendEvent(input).text	
		return ret

#def R2sProxy():
#    input = json.loads(request.data)
#    r2slib = R2sLib()
#    ret = r2slib.R2s_SendEvent(input).text
#    return ret
  
@r2api.route('/FileAPI')
class FileAPI(Resource):
#def FileAPI():
	@r2api.expect(input)
	def post(self, input):
		#input = json.loads(request.data)
		input = request.json
		r2slib = R2sLib()
		rootid = r2slib.R2s_GetID();
		Trans = input.get('Trans')
		session = cluster.connect()
		session.execute("USE testapp")
		
		query = SimpleStatement("INSERT INTO files (file_id, file) VALUES (%s, %s)")
		session.execute(query, (uuid.UUID(rootid), Trans))    
		
		del input['Trans']
		session.shutdown()
		ret = r2slib.R2s_Root("http://localhost:5000/FileImportController", json.dumps(input), rootid )
		
		return (ret)
	
@r2api.route('/FileImportController') 
class FileImportController(Resource):
#def FileImportController():
	@r2api.expect(input)
	def post(self):
		#input = json.loads(request.data)
		input = request.json
		r2slib = R2sLib(input)
		rootid = r2slib.R2s_GetRootID()
		param = json.loads(r2slib.R2s_GetParam())
		Authenticate = param.get('Authenticate')
		
		#register OnFinal and OnError
		r2slib.R2s_Final("http://localhost:5000/OnFinal", "R2s_Final")
		r2slib.R2s_Error("http://localhost:5000/OnError", "R2s_Error")
		
		#load transactions into Cassandra
		session = cluster.connect()
		session.execute("USE testapp")
		query = "select * FROM files WHERE file_id = " + rootid
		rows =  session.execute(query)   
		file = rows.one().file

		lines = file.split('\n')
		for line in lines :
			data = line.split(',')
			transactionid = r2slib.R2s_GetID()
			query = SimpleStatement("INSERT INTO transactions (file_id, transaction_id, from_account, to_account, amount, status)  VALUES (%s, %s, %s, %s, %s, %s)")
			session.execute(query, (uuid.UUID(rootid), uuid.UUID(transactionid), data[0], data[1], data[2], "I"))    
	   
		if (Authenticate == "Batch") :
			r2slib.R2s_Subsequent("http://localhost:5000/BatchController", r2slib.R2s_GetParam())
		if (Authenticate == "Transaction") :
			r2slib.R2s_Subsequent("http://localhost:5000/TransactionController", r2slib.R2s_GetParam())
		session.shutdown()
		r2slib.R2s_Release()
		r2slib.R2s_Complete()
		return r2slib.R2s_GetRootID()

@r2api.route('/BatchController')
class BatchController(Resource):
#def BatchController():
	@r2api.expect(input)
	def post(self):
		#input = json.loads(request.data)
		input = request.json
		r2slib = R2sLib(input)
		rootid = r2slib.R2s_GetRootID()
		param = json.loads(r2slib.R2s_GetParam())
		r2slib.R2s_Subsequent("http://localhost:5000/EvaluateBatch", r2slib.R2s_GetParam())
		session = cluster.connect()
		session.execute("USE testapp")
		query = "select * FROM transactions WHERE file_id = " + rootid
		rows =  session.execute(query) 
		for row in rows :
			transactionid = str(row.transaction_id)
			r2slib.R2s_Contained("http://localhost:5000/AuthTransaction", transactionid, transactionid)

		session.shutdown()
		r2slib.R2s_Release()
		r2slib.R2s_Complete()
		return r2slib.R2s_GetRootID()

@r2api.route('/EvaluateBatch')
#def EvaluateBatch():
class EvaluateBatch(Resource):

	@r2api.expect(input)
	def post():
		#input = json.loads(request.data)
		input = request.json
		r2slib = R2sLib(input)
		rootid = r2slib.R2s_GetRootID()
		param = json.loads(r2slib.R2s_GetParam())
		Clearing = param.get('Clearing')
		session = cluster.connect()
		session.execute("USE testapp")
		query = "select * FROM transactions WHERE file_id = " + rootid
		rows =  session.execute(query) 
		badtransaction = 0
		for row in rows :
			tstatus = row.status
			if tstatus != 'V' : badtransaction = 1
		if badtransaction == 1 : print("File Failed")
		else :
			if (Clearing == "Bulk") : 
				r2slib.R2s_Subsequent("http://localhost:5000/ClearBulk", rootid)
			if (Clearing == "Individual") :
				rows =  session.execute(query) 
				for row in rows :
					transid = str(row.transaction_id)
					r2slib.R2s_Independent("http://localhost:5000/ClearIndividual", transid);						

		session.shutdown()
		r2slib.R2s_Release()
		r2slib.R2s_Complete()
		return r2slib.R2s_GetRootID()



#@app.route('/ClearBulk', methods=['POST'])
@r2api.route('/ClearBulk')
class ClearBulk(Resource):

	@r2api.expect(input)
	def post():
		input = json.loads(request.data)
		r2slib = R2sLib(input)
		rootid = r2slib.R2s_GetRootID()
		session = cluster.connect()
		session.execute("USE testapp")
		query = "select * FROM transactions WHERE file_id = " + rootid
		rows =  session.execute(query) 
		for row in rows :
			tstatus = row.status
			trans = str(row.transaction_id)
			if tstatus == 'V' : 
				query = "UPDATE transactions SET status  = 'C'  WHERE file_id = " + rootid + " AND transaction_id = " + trans;
				session.execute(query)
		session.shutdown()
		r2slib.R2s_Complete()
		return r2slib.R2s_GetRootID()

#@app.route('/ClearIndividual', methods=['POST'])
@r2api.route('/ClearIndividual')
class ClearIndividual(Resource):

	
	@r2api.expect(input)
	def post():
		input = json.loads(request.data)
		r2slib = R2sLib(input)
		rootid = r2slib.R2s_GetRootID()
		param = r2slib.R2s_GetParam()


		session = cluster.connect()
		session.execute("USE testapp")
		query = "select * FROM transactions WHERE file_id = " + rootid + " AND transaction_id = " + param
		rows =  session.execute(query) 
		row = rows.one()
		tstatus = row.status
		if tstatus == "V" :
			query = "UPDATE transactions SET status  = 'C'  WHERE file_id = " + rootid + " AND transaction_id = " + param
			session.execute(query)

		session.shutdown()
		r2slib.R2s_Complete()
		return r2slib.R2s_GetRootID()



#app.route('/TransactionController', methods=['POST'])
@r2api.route('/TransactionController')
class TransactionController(Resource):


	@r2api.expect(input)
	def post():
		input = json.loads(request.data)
		r2slib = R2sLib(input)
		rootid = r2slib.R2s_GetRootID()
		param = json.loads(r2slib.R2s_GetParam())
		Clear = param.get('Clearing')

		session = cluster.connect()
		session.execute("USE testapp")
		if (Clear == "Bulk") : 
			r2slib.R2s_Subsequent("http://localhost:5000/ClearBulk", rootid)
			query = "select * FROM transactions WHERE file_id = " + rootid
			rows =  session.execute(query) 
			for row in rows :
				trans = str(row.transaction_id)
				r2slib.R2s_Contained("http://localhost:5000/AuthTransaction", trans, trans)
		if (Clear == "Individual") : 
			query = "select * FROM transactions WHERE file_id = " + rootid
			rows =  session.execute(query) 
			for row in rows :
				trans = str(row.transaction_id)
				r2slib.R2s_Subsequent("http://localhost:5000/AuthTransaction", trans, trans)
				clearid = r2slib.R2s_Independent("http://localhost:5000/ClearIndividual", trans)
				r2slib.R2s_Setpredecessor(trans, clearid)

		session.shutdown()
		r2slib.R2s_Release()
		r2slib.R2s_Complete()
		return r2slib.R2s_GetRootID()


#@app.route('/AuthTransaction', methods=['POST'])
@r2api.route('/AuthTransaction')
class AuthTransaction(Resource):


	@r2api.expect(input)
	def post():
		input = json.loads(request.data)
		r2slib = R2sLib(input)
		fileid = r2slib.R2s_GetRootID()
		transactionid = r2slib.R2s_GetServiceID()
		session = cluster.connect()
		session.execute("USE testapp")
		query = "select * FROM transactions WHERE file_id = " + fileid + " AND transaction_id = " + transactionid
		rows =  session.execute(query) 
		row = rows.one()
		newstatus = "V"
		fromaccount = row.from_account
		toaccount = row.to_account
		try :
			ifromaccount = int(fromaccount)
			itoaccount = int(toaccount)
		except ValueError :
			newstatus = "F"
		query = "UPDATE transactions SET status = '" + newstatus + "' WHERE file_id = " + fileid + " AND transaction_id = " + transactionid
		session.execute(query)
			
		session.shutdown()
		r2slib.R2s_Complete()
		return r2slib.R2s_GetRootID()


#@app.route('/OnFinal', methods=['POST'])
@r2api.route('/OnFinal')
class OnFinal(Resource):


	@r2api.expect(input)
	def post():

		print("OnFinal")
		input = json.loads(request.data)
		r2slib = R2sLib(input)
		r2slib.R2s_Complete()
		return r2slib.R2s_GetRootID()




#@app.route('/OnError', methods=['POST'])
@r2api.route('/OnError')
class OnError(Resource):


	@r2api.expect(input)
	def post():
		print("OnError")
		input = json.loads(request.data)
		r2slib = R2sLib(input)
		r2slib.R2s_Complete()
		return r2slib.R2s_GetRootID()



#app.run()
#r2api.run()
flask_app.run()
