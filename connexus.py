from google.appengine.api import users
from google.appengine.ext import ndb
from google.appengine.api import mail
from google.appengine.api import images

import jinja2
import webapp2
import urllib
import os
import re

JINJA_ENVIRONMENT = jinja2.Environment(
    loader = jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions = ['jinja2.ext.autoescape'],
    autoescape = True
	)

class Stream(ndb.Model):
	author_email = ndb.StringProperty()
	name = ndb.StringProperty()
	tag = ndb.StringProperty()
	cover_url = ndb.StringProperty()	

class Image(ndb.Model):
	stream = ndb.StructuredProperty(Stream)
	date = ndb.DateTimeProperty(auto_now_add=True)
	image = ndb.BlobProperty()
	comment = ndb.StringProperty()

class Subscriber(ndb.Model):
	stream = ndb.StructuredProperty(Stream)
	email = ndb.StringProperty()

class View(ndb.Model):
	stream = ndb.StructuredProperty(Stream)
	time = ndb.DateTimeProperty(auto_now_add=True)
	comment = ndb.StringProperty()
	
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
		streams = Stream().query(Stream.author_email == users.get_current_user().email())
		template_values = {
		'streams' : streams
		}
		template = JINJA_ENVIRONMENT.get_template('/templates/manage.html')
		self.response.write(template.render(template_values))

class DeleteStream(webapp2.RequestHandler):
	def post(self):
		streams = Stream().query(Stream.author_email == users.get_current_user().email())
		for stream in streams:
			i = self.request.get(stream.name)
			if i:
				if i == 'on':
					stream.key.delete()
		self.redirect('/manage') 


class Create(webapp2.RequestHandler):
	def post(self):
		stream = Stream()
		if users.get_current_user():
			user = users.get_current_user()
			stream.author_email = user.email()
		else:
			stream.author_email = 'Default User'

		stream.name = self.request.get('stream_name')
		stream.tag = self.request.get('tags')
		stream.cover_url = self.request.get('cover_url')
		stream.put()

		for email in re.split('\s*,\s*', self.request.get('receipients')):
			subscriber = Subscriber()
			subscriber.stream = stream
			subscriber.email = email
			subscriber.put()
		
		self.redirect('/stream?' + urllib.urlencode( { 'name' : stream.name }))

	def get(self):
		template_values = {}
		template = JINJA_ENVIRONMENT.get_template('/templates/create.html')
		self.response.write(template.render(template_values))
		
class ViewStream(webapp2.RequestHandler):
    def get(self):
        stream_name = self.request.get('name')
        images = Image.query(Image.stream.name == stream_name).fetch()
        template_values = { 'images' : images,
                            'stream_name' : stream_name }
        template = JINJA_ENVIRONMENT.get_template('/templates/stream.html')
        self.response.write(template.render(template_values))

    def post(self):
        image = Image()
        image.comment = self.request.get('comment')
        raw_image = self.request.get('img')
        raw_image = images.resize(raw_image, 200, 200)
        image.image = raw_image
        image.stream = Stream.query(Stream.name == self.request.get('name')).fetch()[0]

        image.put()

        self.redirect('/stream?' + urllib.urlencode( { 'name' : self.request.get('name') } ))
		
class ViewAll(webapp2.RequestHandler):
	def get(self):
		template_values = { 'streams' : Stream.query() }
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

class GetImage(webapp2.RequestHandler):
    def get(self):
        image = ndb.Key(urlsafe=self.request.get('img_id')).get()
        self.response.headers['Content-Type'] = 'image/png'
        self.response.out.write(image.image)
		
		
app = webapp2.WSGIApplication([
    ('/', MainPage),
	('/manage', Manage),
	('/deleteStream', DeleteStream),
	('/create', Create),
	('/stream', ViewStream),
	('/view_all', ViewAll),
	('/search', Search),
	('/trending', Trending),
	('/error', Error),
	('/img', GetImage)
	
], debug=True)
