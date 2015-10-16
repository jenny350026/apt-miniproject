from google.appengine.api import users
from google.appengine.ext import ndb
from google.appengine.api import mail
from google.appengine.api import images

from collections import Counter
from datetime import datetime, timedelta
from random import randint

import logging
import jinja2
import webapp2
import urllib
import urllib2
import os
import re
import time
import json


JINJA_ENVIRONMENT = jinja2.Environment(
    loader = jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions = ['jinja2.ext.autoescape'],
    autoescape = True
    )

class Stream(ndb.Model):
    user = ndb.UserProperty()
    name = ndb.StringProperty()
    tags = ndb.StringProperty(repeated=True)
    cover_url = ndb.StringProperty()    
    create_time = ndb.DateTimeProperty(auto_now_add = True)
    last_update_time = ndb.DateTimeProperty()
    num_picture = ndb.IntegerProperty(indexed = False)
    view_count = ndb.IntegerProperty()
    trending_view_count = ndb.IntegerProperty()

class Image(ndb.Model):
    stream = ndb.KeyProperty(kind = Stream)
    date = ndb.DateTimeProperty(auto_now_add = True)
    image = ndb.BlobProperty()
    latitude = ndb.FloatProperty()
    longitude = ndb.FloatProperty()
    comment = ndb.StringProperty()

class Subscriber(ndb.Model):
    stream = ndb.KeyProperty(kind = Stream)
    email = ndb.StringProperty()

class View(ndb.Model):
    stream = ndb.KeyProperty(kind = Stream)
    time = ndb.DateTimeProperty(auto_now_add = True)

class UserOption(ndb.Model):
    user = ndb.UserProperty()
    option = ndb.IntegerProperty()
    
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
        logging.info('Starting Main handler')
        # query all streams that the user owns, updates the last_update_time and num_pictures in the stream
        streams = Stream().query(Stream.user == users.get_current_user()).order(Stream.create_time)
        for stream in streams:
            self.update_last_update_time(stream)
            self.update_num_picture(stream)
            stream.put()

        # query all streams that the user subscribed to, updates the last_update_time, num_pictures in the stream, and view_count for the stream
        subscribed = Subscriber().query(Subscriber.email == users.get_current_user().email())
        for sub in subscribed:        
            stream = sub.stream.get()
            self.update_last_update_time(stream)
            self.update_num_picture(stream)
            self.update_view_count(stream)
            stream.put()

        template_values = {
            'streams' : streams,
            'subscribed' : subscribed
        }

        template = JINJA_ENVIRONMENT.get_template('/templates/manage.html')
        self.response.write(template.render(template_values))

    # helper functions
    def update_last_update_time(self, stream):
       update_time = Image.query(Image.stream == stream.key).order(-Image.date).fetch(1)
       if len(update_time) > 0:
           stream.last_update_time = update_time[0].date 
       else:
           stream.last_update_time = stream.create_time    

    def update_num_picture(self, stream):
        stream.num_picture = Image.query(Image.stream == stream.key).count(limit=None)

    def update_view_count(self, stream):
        stream.view_count = View.query(View.stream == stream.key).count(limit=None)

class DeleteStream(webapp2.RequestHandler):
    def post(self):
        stream_ids = self.request.get_all('stream_id') 
        for stream_id in stream_ids:
            stream = ndb.Key(urlsafe=stream_id).get()      
            # delete all subcriber, view and image entries to the stream
            ndb.delete_multi(map(lambda x:x.key, Subscriber.query(Subscriber.stream == stream.key).fetch()))
            ndb.delete_multi(map(lambda x:x.key, View.query(View.stream == stream.key).fetch()))
            ndb.delete_multi(map(lambda x:x.key, Image.query(Image.stream == stream.key).fetch()))
            stream.key.delete()
        time.sleep(1)
        self.response.headers['Content-Type'] = 'text/plain'
        self.response.write('Success')

        # streams = Stream().query(Stream.user == users.get_current_user())
        # for stream in streams:
        #     stream_name = self.request.get(stream.name)
        #     if stream_name and stream_name == 'on':
        #         # delete all subcriber, view and image entries to the stream
        #         ndb.delete_multi(map(lambda x:x.key, Subscriber.query(Subscriber.stream == stream.key).fetch()))
        #         ndb.delete_multi(map(lambda x:x.key, View.query(View.stream == stream.key).fetch()))
        #         ndb.delete_multi(map(lambda x:x.key, Image.query(Image.stream == stream.key).fetch()))
        #         stream.key.delete()
        # time.sleep(1)
        # self.redirect('/manage') 

class Unsubscribe(webapp2.RequestHandler):
    def post(self):
        unsub_streams = self.request.get_all('stream_id') 
        subscribed_streams = Subscriber().query(Subscriber.email == users.get_current_user().email())
        for subscriber in subscribed_streams.fetch():
            stream_id = subscriber.stream
            if stream_id and stream_id in unsub_streams:
                subscriber.key.delete()
        self.response.headers['Content-Type'] = 'text/plain'
        self.response.write('Success')
        

class Create(webapp2.RequestHandler):
    def post(self):
        # if a stream_name is not given, return to /create with error mesage
        if len(self.request.get('stream_name')) == 0:
            template = JINJA_ENVIRONMENT.get_template('/templates/create.html')
            self.response.write(template.render({ 'no_name_error' : "stream name required" }))

        # if a stream with the same name exists, go to error page
        elif len(Stream.query(Stream.name == self.request.get('stream_name')).fetch()) > 0:
            self.redirect('/error')
        else:
            stream = Stream()
            stream.user = users.get_current_user()
            stream.name = self.request.get('stream_name')
            tags = self.request.get('tags')
            stream.tags = [x.strip() for x in tags.split(',')]
            stream.cover_url = self.request.get('cover_url')
            stream.put()

            email_list = [email for email in re.split('\s*,\s*', self.request.get('receipients')) if email]
            for email in email_list:
                subscriber = Subscriber()
                subscriber.stream = stream.key
                subscriber.email = email
                subscriber.put()

            # send email to subscribers 
            if len(email_list) > 0:
                owner_message = self.request.get('message')
                mail.send_mail( sender = "Connexus-info <info@apt-miniproject-1078.appspotmail.com>",
                                to = ','.join(email_list),
                                subject = users.get_current_user().nickname() + " subscribed you to " + stream.name + " stream on Connexus!",
                                body = """
You are now subscribed to %s stream on Connexus! 
Message from %s: 
%s

Connexus: http://apt-miniproject-1078.appspot.com/stream?stream_id=%s""" % (stream.name, stream.user.nickname(), owner_message, stream.key.urlsafe()))

            time.sleep(1)
            self.redirect('/stream?' + urllib.urlencode({ 'stream_name':stream.name }))

    def get(self):
        template_values = {}
        template = JINJA_ENVIRONMENT.get_template('/templates/create.html')
        self.response.write(template.render(template_values))
        
class ViewStream(webapp2.RequestHandler):
    def get(self):
        # TODO define action for "more picture" button
        if self.request.get('stream_name'):
            streams = Stream.query(Stream.name == self.request.get('stream_name')).fetch()
            if len(streams) > 0:
                stream_key = streams[0].key
                next_start = self.request.get('next_start')        
                if next_start == "" or next_start == None:
                    next_start = 0
                start = int(next_start)
                stream = stream_key.get()

                # query all images in the current stream
                images = Image.query(Image.stream == stream_key).order(-Image.date).fetch()
                subscribed = Subscriber().query(ndb.AND(
                                                    Subscriber.email == users.get_current_user().email(), 
                                                    Subscriber.stream == stream_key)).fetch()

                # add a new View entry to record number of views, owner view does not count
                view = self.request.get('view')
                is_owner = (stream.user == users.get_current_user())
                if view and not is_owner:
                    v = View()
                    v.stream = stream_key
                    v.put()

                if len(images) < (start + 3):
                    end = len(images)
                    next_start = 0
                else:
                    end = start + 3
                    next_start = end


                stream_link = 'stream?stream_id=' + stream_key.urlsafe()
                template_values = { 'images' : images[start:end],
                                    'start' : start,
                                    'total_pages' : (len(images) + 3)/3,
                                    'all_images' : images,
                                    'no_file_error' : self.request.get('no_file_error'),
                                    'next_start' : next_start,
                                    'share_link' : stream_link,
                                    'subscribed' : subscribed,
                                    'stream' : stream,
                                    'is_owner': is_owner }
                template = JINJA_ENVIRONMENT.get_template('/templates/stream.html')
                self.response.write(template.render(template_values))
            else:
                self.redirect('stream_not_found')
        else:
            self.redirect('/stream_not_foud')


    def post(self):
        next_start = self.request.get('next_start')        
        if next_start == "" or next_start == None:
            next_start = 0
        redirect_dict = { 'stream_id' : self.request.get('stream_id'), 'next_start' : next_start }
        self.redirect('/stream?' + urllib.urlencode( redirect_dict )) 
        


class Upload(webapp2.RequestHandler):
    def post(self):        
        image = Image()
        image.comment = self.request.get('comment')
        longitude = self.request.get('longitude')
        latitude = self.request.get('latitude')
        if longitude:
            image.longitude = float(longitude)
        else:
            image.longitude = randint(-180, 180)

        if latitude:
            image.latitude = float(latitude)
        else:
            image.latitude = randint(-90, 90)

        chrome_upload_stream_name = self.request.get('chrome_upload_stream_name')
        raw_image = None

        if chrome_upload_stream_name:
            stream_key = Stream.query(Stream.name == chrome_upload_stream_name).fetch()[0].key
            raw_image = urllib2.urlopen(self.request.get('img_url')).read()
        else:
            stream_key = ndb.Key(urlsafe = self.request.get('stream_id'))
            raw_image = self.request.get('file')    
        logging.info('uploading files')
        if raw_image:
            # image.image = images.resize(raw_image, 400, 400)
            logging.info('raw_image')
            image.image = raw_image
            image.stream = stream_key
            image.put()
            time.sleep(2)

        self.redirect('/stream?' + urllib.urlencode({ 'stream_name' : stream_key.get().name })) 


class Subscribe(webapp2.RequestHandler):
    def post(self):
        stream_key = ndb.Key(urlsafe = self.request.get('stream_id'))
        # Check if s/he has already subscribed the stream
        subscribed = Subscriber().query(ndb.AND(Subscriber.email == users.get_current_user().email(), 
                                         Subscriber.stream == stream_key))
        self.response.headers['Content-Type'] = 'text/plain'
        if len(subscribed.fetch()):
            for sub in subscribed.fetch():
                sub.key.delete()
            self.response.write('Unsubscribe')
        else:
            subscriber = Subscriber()
            subscriber.email = users.get_current_user().email()
            subscriber.stream = ndb.Key(urlsafe = self.request.get('stream_id')).get().key
            subscriber.put()
            self.response.write('Subscribe')
        
        # self.redirect('/stream?' + urllib.urlencode({ 'stream_id' : stream_key.urlsafe() }))

class SearchRequest(webapp2.RequestHandler):
    def get(self):
        term = self.request.get('term')
        self.response.headers['Content-Type'] = 'application/json'   
        # template_values = { 'streams' : Stream.query().order(-Stream.create_time) }
        streams = Stream().query().order(-Stream.last_update_time).fetch()
        stream_dict = dict()
        for stream in streams:
            if term.lower() in stream.name.lower():
                stream_dict[stream.name] = stream.name
        obj = dict()
        obj['stream_names'] = stream_dict

        self.response.out.write(json.dumps(obj))

        
class ViewAll(webapp2.RequestHandler):
    def get(self):
        template_values = { 'streams' : Stream.query().order(-Stream.create_time) }
        template = JINJA_ENVIRONMENT.get_template('/templates/view.html')
        self.response.write(template.render(template_values))

class AndroidViewAll(webapp2.RequestHandler):
    def get(self):
        streams = Stream.query().order(-Stream.create_time).fetch()
        obj = dict()
        stream_list = []
        for stream in streams:
            stream_list.append({ 'user_email' : stream.user.email(),
                                 'stream_name' : stream.name,
                                 'cover_url' : stream.cover_url
                                })
            
       
        obj['stream'] = stream_list
        self.response.headers['Content-Type'] = 'text/plain'  
        self.response.out.write(json.dumps(obj))

class Search(webapp2.RequestHandler):
    def get(self):
        template_values = {}
        template = JINJA_ENVIRONMENT.get_template('/templates/search.html')
        self.response.write(template.render(template_values))
    def post(self):
        keyword = self.request.get('search')
        result = []
        if keyword:      
            streams = Stream().query().order(-Stream.last_update_time).fetch()
            # TODO possibly improve search with tags etc.
            for stream in streams:
                if keyword.lower() in stream.name.lower():
                    result.append(stream)
                else:
                    for stream_tag in stream.tags:
                        if keyword.lower() in stream_tag.lower():
                            result.append(stream)


        template_values = { 'streams' : result, 
                            'search' : 'on',                           
                            'keyword' : keyword }
        template = JINJA_ENVIRONMENT.get_template('/templates/view.html')
        self.response.write(template.render(template_values))
        
class Trending(webapp2.RequestHandler):
    def get(self):
        streams = Stream.query(ndb.AND(Stream.trending_view_count != 0, Stream.trending_view_count != None)).order(-Stream.trending_view_count).fetch(5)

        # if no setting yet for user, select no report, otherwise select the current user option
        user_option = UserOption.query(UserOption.user == users.get_current_user()).fetch()
        if len(user_option) == 0:
            user_option = UserOption()
            user_option.user = users.get_current_user()
            user_option.option = 0
            user_option.put()
        else:
            user_option = user_option[0]

        template_values = { 'streams' : streams,
                            'user_option' : user_option }
        template = JINJA_ENVIRONMENT.get_template('/templates/trending.html')
        self.response.write(template.render(template_values))

    def post(self):
        frequency = int(self.request.get('email_frequency'))
        user_option = UserOption.query(UserOption.user == users.get_current_user()).fetch()

        if len(user_option) > 0:
            user_option[0].option = frequency
            user_option[0].put()
        # else actually should not be reach
        else:
            user_option = UserOption()
            user_option.user = users.get_current_user()
            user_option.option = frequency
            user_option.put()
        
        time.sleep(1)
        self.redirect('/trending')

class GeoView(webapp2.RequestHandler):
    def get(self):
        streams = Stream.query(Stream.name == self.request.get('stream_name')).fetch()
        if len(streams) > 0:
            stream = streams[0]
            template_values = { 'stream' : stream }
            template = JINJA_ENVIRONMENT.get_template('/templates/geoview.html')
            self.response.write(template.render(template_values))  

class GetImageLocation(webapp2.RequestHandler):
    def get(self):
        query_begin_date = self.request.get('start')
        query_end_date = self.request.get('end')        
        query_begin_date_obj = datetime.strptime(query_begin_date, "%Y-%m-%dT%H:%M:%S.%fZ")
        query_end_date_obj = datetime.strptime(query_end_date, "%Y-%m-%dT%H:%M:%S.%fZ")

        stream_key = ndb.Key(urlsafe = self.request.get('stream_id'))
        stream = stream_key.get()

        image_list = Image.query(Image.stream == stream_key).order(-Image.date).fetch()
        latLng_dict_list = []
        for image in image_list:
            if query_begin_date_obj <= image.date <= query_end_date_obj:
                image_key = image.key.urlsafe()
                latLng_dict = dict()
                latLng_dict['lat'] = image.latitude
                latLng_dict['lng'] = image.longitude
                latLng_dict['content'] = image_key
                latLng_dict_list.append(latLng_dict)

        obj = dict()
        obj['image_location'] = latLng_dict_list
        self.response.headers['Content-Type'] = 'application/json'  
        self.response.out.write(json.dumps(obj))
        
class Error(webapp2.RequestHandler):
    def get(self):
        template_values = {}
        template = JINJA_ENVIRONMENT.get_template('/templates/error.html')
        self.response.write(template.render(template_values))

class StreamNotFound(webapp2.RequestHandler):
    def get(self):
        template_values = {}
        template = JINJA_ENVIRONMENT.get_template('/templates/stream_not_found.html')
        self.response.write(template.render(template_values))

class GetImage(webapp2.RequestHandler):
    def get(self):
        image = ndb.Key(urlsafe=self.request.get('img_id')).get()
        self.response.headers['Content-Type'] = 'image/png'
        self.response.out.write(image.image)

class UpdateTrending(webapp2.RequestHandler):
    def get(self):
        for stream in Stream.query():
            stream.trending_view_count = View.query(ndb.AND(View.time >= (datetime.now() - timedelta(hours=1)),
                                                       View.stream == stream.key)).count(limit=None)
            stream.put()

class EmailTrending(webapp2.RequestHandler):
    def get(self):
        # query all user with the specified frequency option
        frequency = int(self.request.get('frequency'))
        email_list = map(lambda x:x.user.email(), UserOption.query(UserOption.option == frequency).fetch())

        # send email if the list is not empty
        # TODO change body 
        if len(email_list) > 0:
            # first generate email body from template
            template = JINJA_ENVIRONMENT.get_template('/templates/email_trending.html')
            streams = Stream.query(ndb.AND(Stream.trending_view_count != 0, Stream.trending_view_count != None)).order(-Stream.trending_view_count).fetch(5)
            message = mail.EmailMessage(sender = 'Connexus-trending <trending@apt-miniproject-1078.appspotmail.com>',
                                        to = ','.join(email_list),
                                        subject = 'Top trending streams on Connexus')
            message.html = template.render({'streams' : streams })

            message.send()

class Social(webapp2.RequestHandler):
    def get(self):
        template_values = {}
        template = JINJA_ENVIRONMENT.get_template('/templates/social.html')
        self.response.write(template.render(template_values))

class ChromeExtension(webapp2.RequestHandler):
    def get(self):
        image_name = self.request.get('img_url')
        template_values = {
            'img_url': image_name
        }
        template = JINJA_ENVIRONMENT.get_template('/templates/chrome_extension.html')
        self.response.write(template.render(template_values))

class CheckStreamName(webapp2.RequestHandler):
    def get(self):
        stream_name = self.request.get('stream_name')
        logging.info("calling check stream" + stream_name)
        self.response.headers['Content-Type'] = 'text/plain'
        streams = Stream.query(Stream.name == stream_name).fetch()
        if(len(streams) > 0):
            if(users.get_current_user() == streams[0].user):
                self.response.write('Success')
            else:
                self.response.write('No permission')
        else:
            self.response.write('No stream')
            
        
app = webapp2.WSGIApplication([
    ('/', MainPage),
    ('/manage', Manage),
    ('/deleteStream', DeleteStream),
    ('/unsubscribe', Unsubscribe),
    ('/create', Create),
    ('/stream', ViewStream),
    ('/upload', Upload),
    ('/subscribe', Subscribe),
    ('/view_all', ViewAll),
    ('/search', Search),
    ('/search_request', SearchRequest),
    ('/trending', Trending),
    ('/error', Error),
    ('/img', GetImage),
    ('/tasks/update_trending', UpdateTrending),
    ('/tasks/email_trending', EmailTrending),
    ('/geoview', GeoView),
    ('/get_image_location', GetImageLocation),
    ('/social', Social),
    ('/chrome_extension', ChromeExtension),
    ('/check_stream_name', CheckStreamName),
    ('/stream_not_found', StreamNotFound),
    ('/api/view_all', AndroidViewAll)
    
], debug=True)
