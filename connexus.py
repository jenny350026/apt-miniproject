from google.appengine.api import users
from google.appengine.ext import ndb
from google.appengine.api import mail
from google.appengine.api import images

import jinja2
import webapp2
import os

JINJA_ENVIRONMENT = jinja2.Environment(
    loader = jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions = ['jinja2.ext.autoescape'],
    autoescape = True
	)
	
class MainPage(webapp2.RequestHandler):
	def get(self):
		user = users.get_current_user()
		if user:
			url = users.create_logout_url(self.request.uri)
			url_linktext = 'Logout'
			logged_in = True
		else:
			url = users.create_login_url(self.request.uri)
			url_linktext = 'Login with Google'
			logged_in = False
		
		template_values = {
			'url' : url,
			'url_linktext' : url_linktext,
			'logged_in': logged_in
		}
		template = JINJA_ENVIRONMENT.get_template('/templates/login.html')
		self.response.write(template.render(template_values))
		
class Manage(webapp2.RequestHandler):
	def get(self):
			template = JINJA_ENVIRONMENT.get_template('/templates/manage.html')
			self.response.write(template.render({}))

class Create(webapp2.RequestHandler):
	def get(self):
		template_values = {}
		template = JINJA_ENVIRONMENT.get_template('/templates/create.html')
		self.response.write(template.render(template_values))
		
class Stream(webapp2.RequestHandler):
	def get(self):
		template_values = {}
		template = JINJA_ENVIRONMENT.get_template('/templates/stream.html')
		self.response.write(template.render(template_values))
		
class View(webapp2.RequestHandler):
	def get(self):
		template_values = {}
		template = JINJA_ENVIRONMENT.get_template('/templates/view.html')
		self.response.write(template.render(template_values))
		
class Search(webapp2.RequestHandler):
	def get(self):
		template_values = {}
		template = JINJA_ENVIRONMENT.get_template('/templates/search.html')
		self.response.write(template.render(template_values))
		
class Trending(webapp2.RequestHandler):
	def get(self):
		template_values = {}
		template = JINJA_ENVIRONMENT.get_template('/templates/trending.html')
		self.response.write(template.render(template_values))
		
class Error(webapp2.RequestHandler):
	def get(self):
		template_values = {}
		template = JINJA_ENVIRONMENT.get_template('/templates/error.html')
		self.response.write(template.render(template_values))

class Image(webapp2.RequestHandler):
    def get(self):
        self.response.headers['Content-Type'] = 'image/png'
        self.response.out.write('No image')
		
		
app = webapp2.WSGIApplication([
    ('/', MainPage),
	('/manage', Manage),
	('/create', Create),
	('/stream', Stream),
	('/view', View),
	('/search', Search),
	('/trending', Trending),
	('/error', Error),
	('/img', Image)
	
], debug=True)
