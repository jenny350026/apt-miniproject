from google.appengine.api import users
from google.appengine.ext import ndb
from google.appengine.api import mail
from google.appengine.api import images

import logging
import jinja2
import webapp2
import urllib
import os
import re
import time

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
	create_time = ndb.DateTimeProperty(auto_now_add = True)
	last_update_time = ndb.DateTimeProperty(indexed = False)
	num_picture = ndb.IntegerProperty(indexed = False)

class Image(ndb.Model):
	stream = ndb.KeyProperty(kind = Stream)
	date = ndb.DateTimeProperty(auto_now_add = True)
	image = ndb.BlobProperty()
	comment = ndb.StringProperty()

class Subscriber(ndb.Model):
	stream = ndb.KeyProperty(kind = Stream)
	email = ndb.StringProperty()

class View(ndb.Model):
	stream = ndb.KeyProperty(kind = Stream)
	time = ndb.DateTimeProperty(auto_now_add = True)
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
		for stream in streams:
			update_time = Image.query(Image.stream == stream.key).order(-Image.date).fetch(1)
			if len(update_time) > 0:
				stream.last_update_time = update_time[0].date 
			else:
				stream.last_update_time = stream.create_time	

			stream.num_picture = Image.query(Image.stream == stream.key).count(limit=None)
			stream.put()

		subscribers = Subscriber().query(Subscriber.email == users.get_current_user().email())
		#subscribers.fetch(1)[0].stream.number = 10
		#subscribers.fetch(1)[0].put()

		template_values = {
			'streams' : streams,
			'subscribers' : subscribers
		}

		template = JINJA_ENVIRONMENT.get_template('/templates/manage.html')
		self.response.write(template.render(template_values))

class DeleteStream(webapp2.RequestHandler):
	def post(self):
		streams = Stream().query(Stream.author_email == users.get_current_user().email())
		for stream in streams:
			stream_name = self.request.get(stream.name)
			if stream_name and stream_name == 'on':
				stream.key.delete()
		time.sleep(0.1)
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
			subscriber.stream = stream.key
			subscriber.email = email
			subscriber.put()
		
		self.redirect('/stream?' + urllib.urlencode( { 'stream_id' : stream.key.urlsafe() }))

	def get(self):
		template_values = {}
		template = JINJA_ENVIRONMENT.get_template('/templates/create.html')
		self.response.write(template.render(template_values))
		
class ViewStream(webapp2.RequestHandler):
    def get(self):
        stream_key = ndb.Key(urlsafe = self.request.get('stream_id'))
        stream = stream_key.get()
        images = Image.query(Image.stream == stream_key).order(-Image.date).fetch()
        template_values = { 'images' : images,
                            'stream' : stream }
        template = JINJA_ENVIRONMENT.get_template('/templates/stream.html')
        self.response.write(template.render(template_values))

    def post(self):
    	upload = self.request.get('upload')   
    	subscribe = self.request.get('subscribe')
    	logging.info('Starting Main handler %s', subscribe)
    	if subscribe == 'on':
			logging.info('subscribe == on')
			subscriber = Subscriber()
			subscriber.email = users.get_current_user().email()
			subscriber.stream = ndb.Key(urlsafe = self.request.get('stream_id')).get().key
			subscriber.put()
			self.redirect('/stream?' + urllib.urlencode( { 'stream_id' : subscriber.stream.urlsafe() })) 
    	if upload == 'on':
    		logging.info('upload == on')
	        image = Image()
	        image.comment = self.request.get('comment')
	        raw_image = self.request.get('img')
	        stream_key = ndb.Key(urlsafe = self.request.get('stream_id'))
	        image_list = Image.query(Image.stream == stream_key).order(-Image.date).fetch()    
	        if raw_image:
	        	image.image = images.resize(raw_image, 200, 200)
		        image.stream = stream_key

		        image.put()
		        time.sleep(1)
		        template_values = { 'images' : image_list,
	                            	'stream' : stream_key.get() }
	        else:
		        template_values = { 'images' : image_list,
		        					'no_file_error' : "no file",        					
	                            	'stream' : stream_key.get() }
	    
	        template = JINJA_ENVIRONMENT.get_template('/templates/stream.html')
	        self.response.write(template.render(template_values))

		

		
class ViewAll(webapp2.RequestHandler):
	def get(self):
		template_values = { 'streams' : Stream.query().order(-Stream.create_time) }
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
