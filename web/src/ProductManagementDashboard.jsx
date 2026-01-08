import React, { useState } from 'react';
import { BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Calendar, TrendingUp, Bug, Users, Target, CheckCircle } from 'lucide-react';

export default function ProductManagementDashboard() {
  const [activeTab, setActiveTab] = useState('overview');
  
  // Data Visualization State
  const [features, setFeatures] = useState([
    { id: 1, name: 'User Authentication', completed: true },
    { id: 2, name: 'Dashboard UI', completed: true },
    { id: 3, name: 'API Integration', completed: true },
    { id: 4, name: 'Mobile Responsive Design', completed: false },
    { id: 5, name: 'Analytics Module', completed: false },
  ]);
  
  const [bugs, setBugs] = useState([
    { id: 1, title: 'Login page crashes on Safari', severity: 'Critical' },
    { id: 2, title: 'Data export fails for large datasets', severity: 'Critical' },
    { id: 3, title: 'Mobile menu not responsive', severity: 'Critical' },
    { id: 4, title: 'Search results pagination broken', severity: 'High' },
    { id: 5, title: 'Chart tooltips misaligned', severity: 'Medium' },
  ]);
  
  const [totalCustomers, setTotalCustomers] = useState(820);
  const [monthlyGrowth, setMonthlyGrowth] = useState(140);
  
  const [featureData, setFeatureData] = useState([
    { month: 'Jan', completed: 12, planned: 15 },
    { month: 'Feb', completed: 18, planned: 20 },
    { month: 'Mar', completed: 15, planned: 18 },
    { month: 'Apr', completed: 22, planned: 25 },
  ]);
  
  const [customerData, setCustomerData] = useState([
    { month: 'Jan', customers: 450 },
    { month: 'Feb', customers: 520 },
    { month: 'Mar', customers: 680 },
    { month: 'Apr', customers: 820 },
  ]);
  
  // Roadmap State
  const [roadmapPhases, setRoadmapPhases] = useState([
    { id: 1, phase: 'Planning', start: '2026-01-15', end: '2026-02-28', details: 'Market research, user interviews, competitive analysis', color: 'bg-blue-500' },
    { id: 2, phase: 'Development', start: '2026-03-01', end: '2026-05-31', details: 'Feature implementation, sprint cycles, code reviews', color: 'bg-purple-500' },
    { id: 3, phase: 'Testing', start: '2026-06-01', end: '2026-07-15', details: 'QA testing, user acceptance testing, bug fixes', color: 'bg-orange-500' },
    { id: 4, phase: 'Launch & Post-Launch', start: '2026-07-16', end: '2026-09-30', details: 'Product launch, monitoring, user feedback collection', color: 'bg-green-500' },
  ]);
  
  // Feature Rollout Timeline State
  const [featureRollout, setFeatureRollout] = useState([
    { id: 1, date: '2026-03-15', feature: 'User Dashboard V2' },
    { id: 2, date: '2026-04-20', feature: 'Advanced Analytics' },
    { id: 3, date: '2026-06-01', feature: 'Mobile App Launch' },
    { id: 4, date: '2026-07-16', feature: 'AI-Powered Recommendations' },
  ]);
  
  // SCRUM State
  const [sprintPlan, setSprintPlan] = useState({
    goals: 'Complete user authentication and profile management features',
    userStories: 'As a user, I want to securely log in\nAs a user, I want to update my profile',
    capacity: '80 story points',
  });
  
  const [standupNotes, setStandupNotes] = useState({
    yesterday: 'Completed API integration for user profiles',
    today: 'Working on frontend components for profile editing',
    blockers: 'Waiting for design approval on new UI elements',
  });
  
  const [retrospective, setRetrospective] = useState({
    wentWell: 'Team collaboration was excellent\nDelivered all committed stories',
    improve: 'Better estimation needed\nMore thorough testing before deployment',
    actionItems: 'Schedule estimation workshop\nImplement automated testing pipeline',
  });

  const updateRoadmapPhase = (id, field, value) => {
    setRoadmapPhases(roadmapPhases.map(phase => 
      phase.id === id ? { ...phase, [field]: value } : phase
    ));
  };

  const updateFeatureRollout = (id, field, value) => {
    setFeatureRollout(featureRollout.map(feature => 
      feature.id === id ? { ...feature, [field]: value } : feature
    ));
  };

  const addFeatureRollout = () => {
    const newId = Math.max(...featureRollout.map(f => f.id)) + 1;
    setFeatureRollout([...featureRollout, { id: newId, date: '2026-01-01', feature: 'New Feature' }]);
  };

  const toggleFeature = (id) => {
    setFeatures(features.map(f => f.id === id ? { ...f, completed: !f.completed } : f));
  };

  const addFeature = () => {
    const newId = Math.max(...features.map(f => f.id), 0) + 1;
    setFeatures([...features, { id: newId, name: 'New Feature', completed: false }]);
  };

  const updateFeatureName = (id, name) => {
    setFeatures(features.map(f => f.id === id ? { ...f, name } : f));
  };

  const deleteFeature = (id) => {
    setFeatures(features.filter(f => f.id !== id));
  };

  const resetFeatures = () => {
    setFeatures([]);
  };

  const uncheckAllFeatures = () => {
    setFeatures(features.map(f => ({ ...f, completed: false })));
  };

  const checkAllFeatures = () => {
    setFeatures(features.map(f => ({ ...f, completed: true })));
  };

  const addBug = () => {
    const newId = Math.max(...bugs.map(b => b.id), 0) + 1;
    setBugs([...bugs, { id: newId, title: 'New Bug', severity: 'Medium' }]);
  };

  const updateBug = (id, field, value) => {
    setBugs(bugs.map(b => b.id === id ? { ...b, [field]: value } : b));
  };

  const deleteBug = (id) => {
    setBugs(bugs.filter(b => b.id !== id));
  };

  const resetBugs = () => {
    setBugs([]);
  };

  const clearBugsBySeverity = (severity) => {
    setBugs(bugs.filter(b => b.severity !== severity));
  };

  const completedFeatures = features.filter(f => f.completed).length;
  const totalFeatures = features.length;
  const completionRate = totalFeatures > 0 ? Math.round((completedFeatures / totalFeatures) * 100) : 0;
  
  const criticalBugs = bugs.filter(b => b.severity === 'Critical').length;
  const highBugs = bugs.filter(b => b.severity === 'High').length;
  
  const bugData = [
    { severity: 'Critical', count: bugs.filter(b => b.severity === 'Critical').length },
    { severity: 'High', count: bugs.filter(b => b.severity === 'High').length },
    { severity: 'Medium', count: bugs.filter(b => b.severity === 'Medium').length },
    { severity: 'Low', count: bugs.filter(b => b.severity === 'Low').length },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-gradient-to-r from-blue-600 to-blue-800 text-white p-6 shadow-lg">
        <h1 className="text-3xl font-bold">Product Management Dashboard</h1>
        <p className="text-blue-100 mt-1">Comprehensive toolkit for product managers</p>
      </div>

      <div className="border-b border-gray-200 bg-white shadow-sm">
        <nav className="flex space-x-1 px-6">
          {['overview', 'roadmap', 'rollout', 'scrum'].map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-6 py-4 font-medium text-sm transition-colors ${
                activeTab === tab
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              {tab === 'overview' && 'Data Overview'}
              {tab === 'roadmap' && 'Product Roadmap'}
              {tab === 'rollout' && 'Feature Rollout'}
              {tab === 'scrum' && 'SCRUM & Agile'}
            </button>
          ))}
        </nav>
      </div>

      <div className="p-6 max-w-7xl mx-auto">
        {activeTab === 'overview' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900">Feature Completion</h3>
                  <CheckCircle className="text-blue-600" size={24} />
                </div>
                <div className="text-3xl font-bold text-blue-600">{completionRate}%</div>
                <p className="text-gray-600 text-sm mt-1">{completedFeatures} of {totalFeatures} features completed</p>
                <div className="mt-4 space-y-2">
                  <button
                    onClick={addFeature}
                    className="w-full px-3 py-2 bg-blue-50 text-blue-600 rounded-md hover:bg-blue-100 transition-colors text-sm font-medium"
                  >
                    + Add Feature
                  </button>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={checkAllFeatures}
                      className="px-3 py-2 bg-green-50 text-green-600 rounded-md hover:bg-green-100 transition-colors text-xs font-medium"
                    >
                      Check All
                    </button>
                    <button
                      onClick={uncheckAllFeatures}
                      className="px-3 py-2 bg-gray-100 text-gray-600 rounded-md hover:bg-gray-200 transition-colors text-xs font-medium"
                    >
                      Uncheck All
                    </button>
                  </div>
                  <button
                    onClick={resetFeatures}
                    className="w-full px-3 py-2 bg-red-50 text-red-600 rounded-md hover:bg-red-100 transition-colors text-xs font-medium"
                  >
                    Reset All Features
                  </button>
                </div>
              </div>
              
              <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900">Active Bugs</h3>
                  <Bug className="text-orange-600" size={24} />
                </div>
                <div className="text-3xl font-bold text-orange-600">{bugs.length}</div>
                <p className="text-gray-600 text-sm mt-1">{criticalBugs} critical, {highBugs} high priority</p>
                <div className="mt-4 space-y-2">
                  <button
                    onClick={addBug}
                    className="w-full px-3 py-2 bg-orange-50 text-orange-600 rounded-md hover:bg-orange-100 transition-colors text-sm font-medium"
                  >
                    + Add Bug
                  </button>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={() => clearBugsBySeverity('Critical')}
                      className="px-3 py-2 bg-red-50 text-red-600 rounded-md hover:bg-red-100 transition-colors text-xs font-medium"
                    >
                      Clear Critical
                    </button>
                    <button
                      onClick={() => clearBugsBySeverity('High')}
                      className="px-3 py-2 bg-orange-50 text-orange-600 rounded-md hover:bg-orange-100 transition-colors text-xs font-medium"
                    >
                      Clear High
                    </button>
                  </div>
                  <button
                    onClick={resetBugs}
                    className="w-full px-3 py-2 bg-red-50 text-red-600 rounded-md hover:bg-red-100 transition-colors text-xs font-medium"
                  >
                    Reset All Bugs
                  </button>
                </div>
              </div>
              
              <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900">Customers</h3>
                  <Users className="text-green-600" size={24} />
                </div>
                <div className="text-3xl font-bold text-green-600">{totalCustomers}</div>
                <p className="text-gray-600 text-sm mt-1">+{monthlyGrowth} this month</p>
                <div className="mt-4 space-y-2">
                  <input
                    type="number"
                    value={totalCustomers}
                    onChange={(e) => setTotalCustomers(parseInt(e.target.value) || 0)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                    placeholder="Total customers"
                  />
                  <input
                    type="number"
                    value={monthlyGrowth}
                    onChange={(e) => setMonthlyGrowth(parseInt(e.target.value) || 0)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                    placeholder="Monthly growth"
                  />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Features List</h3>
                <div className="space-y-2 max-h-96 overflow-y-auto">
                  {features.map((feature) => (
                    <div key={feature.id} className="flex items-center gap-2 p-2 border border-gray-200 rounded hover:bg-gray-50">
                      <input
                        type="checkbox"
                        checked={feature.completed}
                        onChange={() => toggleFeature(feature.id)}
                        className="w-5 h-5 text-blue-600 rounded"
                      />
                      <input
                        type="text"
                        value={feature.name}
                        onChange={(e) => updateFeatureName(feature.id, e.target.value)}
                        className="flex-1 px-2 py-1 border-0 focus:ring-2 focus:ring-blue-500 rounded"
                      />
                      <button
                        onClick={() => deleteFeature(feature.id)}
                        className="px-2 py-1 text-red-600 hover:bg-red-50 rounded text-sm"
                      >
                        Delete
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Bug List</h3>
                <div className="space-y-2 max-h-96 overflow-y-auto">
                  {bugs.map((bug) => (
                    <div key={bug.id} className="p-3 border border-gray-200 rounded hover:bg-gray-50">
                      <input
                        type="text"
                        value={bug.title}
                        onChange={(e) => updateBug(bug.id, 'title', e.target.value)}
                        className="w-full px-2 py-1 mb-2 border border-gray-300 rounded text-sm"
                        placeholder="Bug title"
                      />
                      <div className="flex items-center gap-2">
                        <select
                          value={bug.severity}
                          onChange={(e) => updateBug(bug.id, 'severity', e.target.value)}
                          className="flex-1 px-2 py-1 border border-gray-300 rounded text-sm"
                        >
                          <option value="Critical">Critical</option>
                          <option value="High">High</option>
                          <option value="Medium">Medium</option>
                          <option value="Low">Low</option>
                        </select>
                        <button
                          onClick={() => deleteBug(bug.id)}
                          className="px-3 py-1 text-red-600 hover:bg-red-50 rounded text-sm"
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Feature Completion Rate</h3>
                <div className="mb-4 space-y-2">
                  <p className="text-sm font-medium text-gray-700">Enter Data (Month, Completed, Planned):</p>
                  {featureData.map((item, idx) => (
                    <div key={idx} className="flex gap-2">
                      <input
                        type="text"
                        value={item.month}
                        onChange={(e) => {
                          const newData = [...featureData];
                          newData[idx].month = e.target.value;
                          setFeatureData(newData);
                        }}
                        className="w-20 px-2 py-1 text-sm border border-gray-300 rounded"
                        placeholder="Month"
                      />
                      <input
                        type="number"
                        value={item.completed}
                        onChange={(e) => {
                          const newData = [...featureData];
                          newData[idx].completed = parseInt(e.target.value) || 0;
                          setFeatureData(newData);
                        }}
                        className="w-24 px-2 py-1 text-sm border border-gray-300 rounded"
                        placeholder="Completed"
                      />
                      <input
                        type="number"
                        value={item.planned}
                        onChange={(e) => {
                          const newData = [...featureData];
                          newData[idx].planned = parseInt(e.target.value) || 0;
                          setFeatureData(newData);
                        }}
                        className="w-24 px-2 py-1 text-sm border border-gray-300 rounded"
                        placeholder="Planned"
                      />
                    </div>
                  ))}
                </div>
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={featureData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="month" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="completed" fill="#3b82f6" name="Completed" />
                    <Bar dataKey="planned" fill="#93c5fd" name="Planned" />
                  </BarChart>
                </ResponsiveContainer>
              </div>

              <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Bug Distribution by Severity</h3>
                <ResponsiveContainer width="100%" height={350}>
                  <BarChart data={bugData} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis type="number" />
                    <YAxis dataKey="severity" type="category" />
                    <Tooltip />
                    <Bar dataKey="count" fill="#f97316" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Customer Acquisition Growth</h3>
              <div className="mb-4 space-y-2">
                <p className="text-sm font-medium text-gray-700">Enter Data (Month, Customers):</p>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                  {customerData.map((item, idx) => (
                    <div key={idx} className="flex gap-2">
                      <input
                        type="text"
                        value={item.month}
                        onChange={(e) => {
                          const newData = [...customerData];
                          newData[idx].month = e.target.value;
                          setCustomerData(newData);
                        }}
                        className="w-20 px-2 py-1 text-sm border border-gray-300 rounded"
                        placeholder="Month"
                      />
                      <input
                        type="number"
                        value={item.customers}
                        onChange={(e) => {
                          const newData = [...customerData];
                          newData[idx].customers = parseInt(e.target.value) || 0;
                          setCustomerData(newData);
                        }}
                        className="flex-1 px-2 py-1 text-sm border border-gray-300 rounded"
                        placeholder="Customers"
                      />
                    </div>
                  ))}
                </div>
              </div>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={customerData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="customers" stroke="#10b981" strokeWidth={3} name="Total Customers" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {activeTab === 'roadmap' && (
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Product Roadmap - Gantt Chart</h2>
              
              <div className="space-y-4">
                {roadmapPhases.map((phase) => (
                  <div key={phase.id} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-center gap-4 mb-3">
                      <div className={`w-4 h-4 rounded ${phase.color}`}></div>
                      <h3 className="text-lg font-semibold text-gray-900">{phase.phase}</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-3">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
                        <input
                          type="date"
                          value={phase.start}
                          onChange={(e) => updateRoadmapPhase(phase.id, 'start', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
                        <input
                          type="date"
                          value={phase.end}
                          onChange={(e) => updateRoadmapPhase(phase.id, 'end', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                      </div>
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Details</label>
                      <textarea
                        value={phase.details}
                        onChange={(e) => updateRoadmapPhase(phase.id, 'details', e.target.value)}
                        rows={3}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      />
                    </div>
                    
                    <div className="mt-3 h-8 bg-gray-100 rounded relative overflow-hidden">
                      <div className={`${phase.color} h-full opacity-80`} style={{ width: '100%' }}></div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'rollout' && (
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-gray-900">Feature Rollout Timeline</h2>
                <button
                  onClick={addFeatureRollout}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
                >
                  Add Feature
                </button>
              </div>
              
              <div className="space-y-4">
                {featureRollout.map((feature) => (
                  <div key={feature.id} className="flex items-center gap-4 p-4 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors">
                    <Calendar className="text-blue-600" size={24} />
                    <div className="flex-1 grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Date</label>
                        <input
                          type="date"
                          value={feature.date}
                          onChange={(e) => updateFeatureRollout(feature.id, 'date', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Feature Name</label>
                        <input
                          type="text"
                          value={feature.feature}
                          onChange={(e) => updateFeatureRollout(feature.id, 'feature', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'scrum' && (
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">SCRUM Process Overview</h2>
              <div className="flex items-center justify-between p-4 bg-blue-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-600 text-white rounded-full flex items-center justify-center font-bold">1</div>
                  <span className="font-medium">Sprint Planning</span>
                </div>
                <div className="text-2xl text-gray-300">→</div>
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-600 text-white rounded-full flex items-center justify-center font-bold">2</div>
                  <span className="font-medium">Daily Standups</span>
                </div>
                <div className="text-2xl text-gray-300">→</div>
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-600 text-white rounded-full flex items-center justify-center font-bold">3</div>
                  <span className="font-medium">Sprint Review</span>
                </div>
                <div className="text-2xl text-gray-300">→</div>
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-600 text-white rounded-full flex items-center justify-center font-bold">4</div>
                  <span className="font-medium">Retrospective</span>
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Sprint Planning Template</h2>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Sprint Goals</label>
                  <textarea
                    value={sprintPlan.goals}
                    onChange={(e) => setSprintPlan({...sprintPlan, goals: e.target.value})}
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">User Stories</label>
                  <textarea
                    value={sprintPlan.userStories}
                    onChange={(e) => setSprintPlan({...sprintPlan, userStories: e.target.value})}
                    rows={5}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="One user story per line"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Team Capacity</label>
                  <input
                    type="text"
                    value={sprintPlan.capacity}
                    onChange={(e) => setSprintPlan({...sprintPlan, capacity: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Daily Standup Notes</h2>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">What did I do yesterday?</label>
                  <textarea
                    value={standupNotes.yesterday}
                    onChange={(e) => setStandupNotes({...standupNotes, yesterday: e.target.value})}
                    rows={2}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">What will I do today?</label>
                  <textarea
                    value={standupNotes.today}
                    onChange={(e) => setStandupNotes({...standupNotes, today: e.target.value})}
                    rows={2}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Any blockers?</label>
                  <textarea
                    value={standupNotes.blockers}
                    onChange={(e) => setStandupNotes({...standupNotes, blockers: e.target.value})}
                    rows={2}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Sprint Retrospective</h2>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">What went well?</label>
                  <textarea
                    value={retrospective.wentWell}
                    onChange={(e) => setRetrospective({...retrospective, wentWell: e.target.value})}
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="One item per line"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">What could be improved?</label>
                  <textarea
                    value={retrospective.improve}
                    onChange={(e) => setRetrospective({...retrospective, improve: e.target.value})}
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="One item per line"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Action Items</label>
                  <textarea
                    value={retrospective.actionItems}
                    onChange={(e) => setRetrospective({...retrospective, actionItems: e.target.value})}
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="One action item per line"
                  />
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
