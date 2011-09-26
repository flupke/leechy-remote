#!/usr/bin/env python
from setuptools import setup, find_packages
import sys, os

here = os.path.abspath(os.path.dirname(__file__))
README = open(os.path.join(here, 'README.rst')).read()
NEWS = open(os.path.join(here, 'NEWS.txt')).read()


version = '0.1'

install_requires = [
    "gevent", 
    "pybonjour",
]


setup(name='leechyremote',
    version=version,
    description="A remote control system for Leechy.",
    long_description=README + '\n\n' + NEWS,
    classifiers=[
      # Get strings from http://pypi.python.org/pypi?%3Aaction=list_classifiers
    ],
    keywords='',
    author='Luper Rouch',
    author_email='luper.rouch@gmail.com',
    url='https://github.com/flupke/leechy-remote',
    license='MIT License',
    packages=find_packages('leechyremote'),
    package_dir = {'': 'leechyremote'}, 
    include_package_data=True,
    zip_safe=False,
    install_requires=install_requires,
    entry_points={
        'console_scripts':
            ['leechy-remote-server=leechyremote:main']
    }
)
